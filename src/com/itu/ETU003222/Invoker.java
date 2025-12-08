package com.itu.ETU003222;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import javax.servlet.http.HttpServletRequest;

import com.itu.ETU003222.model.Mapping;
import com.itu.ETU003222.annotation.RequestParam;

public class Invoker {
    
    public static Object invoke(Mapping mapping) throws Exception {
        return invoke(mapping, null);
    }
    
    public static Object invoke(Mapping mapping, HttpServletRequest request) throws Exception {
        Class<?> clazz = Class.forName(mapping.getClassName());
        
        Object instance = clazz.getDeclaredConstructor().newInstance();
        
        // Récupérer toutes les méthodes de la classe
        Method[] methods = clazz.getDeclaredMethods();
        Method targetMethod = null;
        
        // Trouver la méthode correspondante
        for (Method method : methods) {
            if (method.getName().equals(mapping.getMethodName())) {
                targetMethod = method;
                break;
            }
        }
        
        if (targetMethod == null) {
            throw new NoSuchMethodException("Méthode non trouvée : " + mapping.getMethodName());
        }
        
        // Récupérer les paramètres de la méthode
        Parameter[] parameters = targetMethod.getParameters();
        
        // Si la méthode n'a pas de paramètres, l'invoquer directement
        if (parameters.length == 0) {
            return targetMethod.invoke(instance);
        }
        
        // Si pas de requête HTTP, impossible de récupérer les paramètres
        if (request == null) {
            throw new IllegalArgumentException("HttpServletRequest requis pour les méthodes avec paramètres");
        }
        
        // Préparer les arguments pour l'invocation
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            // CAS 1 : Vérifier si le paramètre est de type Map (PRIORITÉ HAUTE)
            if (java.util.Map.class.isAssignableFrom(paramType)) {
                // Construire une Map à partir de tous les paramètres de la requête
                java.util.Map<String, Object> paramMap = new java.util.HashMap<>();
                java.util.Map<String, String[]> requestParams = request.getParameterMap();
                
                for (java.util.Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    
                    // Si un seul élément, mettre juste la valeur, sinon mettre le tableau
                    if (values != null && values.length == 1) {
                        paramMap.put(key, values[0]);
                    } else {
                        paramMap.put(key, values);
                    }
                }
                
                args[i] = paramMap;
                continue;
            }
            
            // CAS 2 : Vérifier si c'est un objet métier (binding automatique)
            if (!isPrimitiveOrWrapper(paramType) && !paramType.equals(String.class)) {
                // C'est un objet personnalisé, on va le construire avec les paramètres
                Object objectInstance = bindObject(paramType, param.getName(), request);
                args[i] = objectInstance;
                continue;
            }
            
            String paramValue = null;
            String paramName = null;
            
            // CAS 3 : Vérifier si le paramètre a l'annotation @RequestParam
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                paramName = requestParam.value();
                
                // Si value() est vide, utiliser le nom du paramètre
                if (paramName == null || paramName.isEmpty()) {
                    paramName = param.getName();
                }
                
                // Récupérer la valeur depuis request.getParameter() avec le nom spécifié dans @RequestParam
                paramValue = request.getParameter(paramName);
                
                if (paramValue == null) {
                    throw new IllegalArgumentException(
                        "Paramètre @RequestParam manquant : " + paramName + 
                        " (type: " + paramType.getName() + ")"
                    );
                }
            }
            // CAS 4 : Paramètre simple sans annotation
            else {
                paramName = param.getName();
                
                // Vérifier d'abord si c'est un paramètre de query string normal (?name=value)
                paramValue = request.getParameter(paramName);
                
                // Si null, c'est peut-être un paramètre d'URL pattern
                // Le wrapper dans FrontServlet aura déjà ajouté ces valeurs
                // Donc si paramValue est toujours null ici, c'est vraiment manquant
                
                if (paramValue == null) {
                    throw new IllegalArgumentException(
                        "Paramètre manquant : " + paramName + 
                        " (type: " + paramType.getName() + 
                        "). Vérifiez que le nom du paramètre correspond exactement à celui de l'URL."
                    );
                }
            }
            
            // Convertir la valeur selon le type du paramètre
            args[i] = convertParameter(paramValue, paramType);
        }
        
        return targetMethod.invoke(instance, args);
    }
    
    // Méthode pour vérifier si un type est primitif ou wrapper
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type.equals(Integer.class) || 
               type.equals(Long.class) || 
               type.equals(Double.class) || 
               type.equals(Float.class) || 
               type.equals(Boolean.class) ||
               type.equals(Character.class) ||
               type.equals(Byte.class) ||
               type.equals(Short.class);
    }
    
    // Méthode pour créer et remplir un objet à partir des paramètres de la requête
    private static Object bindObject(Class<?> objectClass, String paramPrefix, HttpServletRequest request) throws Exception {
        // Créer une instance de l'objet
        Object objectInstance = objectClass.getDeclaredConstructor().newInstance();
        
        // Récupérer tous les paramètres de la requête
        java.util.Map<String, String[]> requestParams = request.getParameterMap();
        
        // Pour chaque paramètre, vérifier s'il correspond au pattern "prefix.property"
        for (java.util.Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            
            // Vérifier si le paramètre commence par "prefix."
            if (key.startsWith(paramPrefix + ".")) {
                String propertyName = key.substring((paramPrefix + ".").length());
                String value = (values != null && values.length > 0) ? values[0] : null;
                
                if (value != null && !value.trim().isEmpty()) {
                    // Trouver le setter correspondant
                    String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                    
                    // Chercher le setter dans la classe
                    Method[] methods = objectClass.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                            Class<?> paramType = method.getParameterTypes()[0];
                            Object convertedValue = convertParameter(value, paramType);
                            method.invoke(objectInstance, convertedValue);
                            break;
                        }
                    }
                }
            }
        }
        
        return objectInstance;
    }
    
    // Méthode utilitaire pour convertir les paramètres String vers leur type approprié
    private static Object convertParameter(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            // Pour les types primitifs, retourner des valeurs par défaut
            if (targetType == int.class) return 0;
            if (targetType == long.class) return 0L;
            if (targetType == double.class) return 0.0;
            if (targetType == float.class) return 0.0f;
            if (targetType == boolean.class) return false;
            return null;
        }
        
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        
        // Par défaut, retourner la chaîne
        return value;
    }
}