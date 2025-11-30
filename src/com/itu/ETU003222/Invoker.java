package com.itu.ETU003222;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import com.itu.ETU003222.model.Mapping;

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
        
        // Récupérer tous les paramètres de la requête
        Enumeration<String> paramNames = request.getParameterNames();
        
        // Si on a exactement le même nombre de paramètres dans la requête que dans la méthode
        int paramCount = 0;
        String firstParamName = null;
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if (paramCount == 0) {
                firstParamName = name;
            }
            paramCount++;
        }
        
        // Pour chaque paramètre de la méthode, prendre les valeurs dans l'ordre
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramValue = null;
            
            // Essayer de récupérer par le nom du paramètre (peut être arg0, arg1, etc.)
            paramValue = request.getParameter(param.getName());
            
            // Si pas trouvé et qu'on a un seul paramètre, prendre le premier paramètre de la requête
            if (paramValue == null && parameters.length == 1 && firstParamName != null) {
                paramValue = request.getParameter(firstParamName);
            }
            
            // Convertir la valeur selon le type du paramètre
            Class<?> paramType = param.getType();
            
            if (paramValue == null) {
                throw new IllegalArgumentException("Paramètre manquant pour : " + param.getName() + " (type: " + paramType.getName() + ")");
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