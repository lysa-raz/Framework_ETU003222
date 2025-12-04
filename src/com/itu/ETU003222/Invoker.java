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
            String paramValue = null;
            String paramName = null;
            Class<?> paramType = param.getType();
            
            // CAS 1 : Vérifier si le paramètre a l'annotation @RequestParam
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
            // CAS 2 : Vérifier si c'est un paramètre extrait de l'URL (pattern avec {})
            // Ces paramètres sont déjà dans request.getParameter() grâce au wrapper dans FrontServlet
            else {
                paramName = param.getName();
                
                // Vérifier d'abord si c'est un paramètre de query string normal (?name=value)
                paramValue = request.getParameter(paramName);
                
                // CAS 3 : Si null, c'est peut-être un paramètre d'URL pattern
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