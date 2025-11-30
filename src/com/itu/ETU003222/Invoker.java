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
            String paramName;
            
            // Vérifier si le paramètre a l'annotation @RequestParam
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                paramName = requestParam.value();
                
                // Si value() est vide, utiliser le nom du paramètre
                if (paramName == null || paramName.isEmpty()) {
                    paramName = param.getName();
                }
            } else {
                // Sinon, utiliser le nom du paramètre tel quel
                paramName = param.getName();
            }
            
            String paramValue = request.getParameter(paramName);
            
            // Convertir la valeur selon le type du paramètre
            Class<?> paramType = param.getType();
            
            if (paramValue == null) {
                throw new IllegalArgumentException("Paramètre manquant pour : " + paramName + " (type: " + paramType.getName() + ")");
            }
            
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