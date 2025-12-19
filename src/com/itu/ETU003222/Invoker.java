package com.itu.ETU003222;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.servlet.http.HttpServletRequest;

import com.itu.ETU003222.model.Mapping;
import com.itu.ETU003222.model.FileUpload;
import com.itu.ETU003222.annotation.RequestParam;

public class Invoker {
    
    public static Object invoke(Mapping mapping) throws Exception {
        return invoke(mapping, null);
    }
    
    public static Object invoke(Mapping mapping, HttpServletRequest request) throws Exception {
        Class<?> clazz = Class.forName(mapping.getClassName());
        
        Object instance = clazz.getDeclaredConstructor().newInstance();
        
        Method[] methods = clazz.getDeclaredMethods();
        Method targetMethod = null;
        
        for (Method method : methods) {
            if (method.getName().equals(mapping.getMethodName())) {
                targetMethod = method;
                break;
            }
        }
        
        if (targetMethod == null) {
            throw new NoSuchMethodException("Méthode non trouvée : " + mapping.getMethodName());
        }
        
        Parameter[] parameters = targetMethod.getParameters();
        
        if (parameters.length == 0) {
            return targetMethod.invoke(instance);
        }
        
        if (request == null) {
            throw new IllegalArgumentException("HttpServletRequest requis pour les méthodes avec paramètres");
        }
        
        Object[] args = new Object[parameters.length];
        
        // NOUVEAU : Récupérer les fichiers uploadés
        @SuppressWarnings("unchecked")
        java.util.Map<String, FileUpload> uploadedFiles = 
            (java.util.Map<String, FileUpload>) request.getAttribute("uploadedFiles");
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            // CAS 1 : Map<String, Object> pour les données du formulaire
            if (java.util.Map.class.isAssignableFrom(paramType)) {
                // Vérifier si c'est une Map de fichiers ou de données normales
                Type genericType = param.getParameterizedType();
                boolean isFileMap = false;
                
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) genericType;
                    Type[] typeArgs = pType.getActualTypeArguments();
                    if (typeArgs.length == 2 && typeArgs[1].equals(FileUpload.class)) {
                        isFileMap = true;
                    }
                }
                
                // Si c'est Map<String, FileUpload>, passer les fichiers uploadés
                if (isFileMap) {
                    args[i] = uploadedFiles != null ? uploadedFiles : new java.util.HashMap<String, FileUpload>();
                    continue;
                }
                
                // Sinon, c'est une Map normale pour les données du formulaire
                java.util.Map<String, Object> paramMap = new java.util.HashMap<>();
                java.util.Map<String, String[]> requestParams = request.getParameterMap();
                
                for (java.util.Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    
                    if (values != null && values.length == 1) {
                        paramMap.put(key, values[0]);
                    } else {
                        paramMap.put(key, values);
                    }
                }
                
                args[i] = paramMap;
                continue;
            }
            
            // CAS 2 NOUVEAU : List<FileUpload> pour plusieurs fichiers
            if (java.util.List.class.isAssignableFrom(paramType)) {
                Type genericType = param.getParameterizedType();
                
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) genericType;
                    Type[] typeArgs = pType.getActualTypeArguments();
                    
                    // Si c'est List<FileUpload>
                    if (typeArgs.length == 1 && typeArgs[0].equals(FileUpload.class)) {
                        java.util.List<FileUpload> fileList = new java.util.ArrayList<>();
                        
                        if (uploadedFiles != null) {
                            fileList.addAll(uploadedFiles.values());
                        }
                        
                        args[i] = fileList;
                        continue;
                    }
                }
            }
            
            // CAS 3 : FileUpload pour un fichier unique
            if (paramType.equals(FileUpload.class)) {
                String paramName = param.getName();
                
                if (uploadedFiles != null && uploadedFiles.containsKey(paramName)) {
                    args[i] = uploadedFiles.get(paramName);
                } else {
                    args[i] = null;
                }
                continue;
            }
            
            // CAS 4 : Objet métier (binding automatique)
            if (!isPrimitiveOrWrapper(paramType) && !paramType.equals(String.class)) {
                Object objectInstance = bindObject(paramType, param.getName(), request);
                args[i] = objectInstance;
                continue;
            }
            
            String paramValue = null;
            String paramName = null;
            
            // CAS 5 : @RequestParam
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = param.getAnnotation(RequestParam.class);
                paramName = requestParam.value();
                
                if (paramName == null || paramName.isEmpty()) {
                    paramName = param.getName();
                }
                
                paramValue = request.getParameter(paramName);
                
                if (paramValue == null) {
                    throw new IllegalArgumentException(
                        "Paramètre @RequestParam manquant : " + paramName + 
                        " (type: " + paramType.getName() + ")"
                    );
                }
            }
            // CAS 6 : Paramètre simple
            else {
                paramName = param.getName();
                paramValue = request.getParameter(paramName);
                
                if (paramValue == null) {
                    throw new IllegalArgumentException(
                        "Paramètre manquant : " + paramName + 
                        " (type: " + paramType.getName() + ")"
                    );
                }
            }
            
            args[i] = convertParameter(paramValue, paramType);
        }
        
        return targetMethod.invoke(instance, args);
    }
    
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
    
    private static Object bindObject(Class<?> objectClass, String paramPrefix, HttpServletRequest request) throws Exception {
        Object objectInstance = objectClass.getDeclaredConstructor().newInstance();
        java.util.Map<String, String[]> requestParams = request.getParameterMap();
        
        for (java.util.Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            
            if (key.startsWith(paramPrefix + ".")) {
                String propertyName = key.substring((paramPrefix + ".").length());
                String value = (values != null && values.length > 0) ? values[0] : null;
                
                if (value != null && !value.trim().isEmpty()) {
                    String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                    
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
    
    private static Object convertParameter(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
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
        
        return value;
    }
}