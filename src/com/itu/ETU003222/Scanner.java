package com.itu.ETU003222;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.itu.ETU003222.annotation.AnnotationController;
import com.itu.ETU003222.annotation.UrlMapping;
import com.itu.ETU003222.annotation.GetMapping;
import com.itu.ETU003222.annotation.PostMapping;
import com.itu.ETU003222.annotation.RestApi;
import com.itu.ETU003222.model.Mapping;

import java.lang.reflect.Method;

public class Scanner {
    
    public static HashMap<String, Mapping> scanControllers(String packageName) throws Exception {
        HashMap<String, Mapping> mappings = new HashMap<>();
        
        // Récupérer toutes les classes du package
        List<Class<?>> classes = getClasses(packageName);
        
        for (Class<?> clazz : classes) {
            // Vérifier si la classe a l'annotation @AnnotationController
            if (clazz.isAnnotationPresent(AnnotationController.class)) {
                // Scanner toutes les méthodes de cette classe
                Method[] methods = clazz.getDeclaredMethods();
                
                for (Method method : methods) {
                    // Vérifier si la méthode a l'annotation @UrlMapping
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);
                        String url = urlMapping.value();
                        String httpMethod = urlMapping.method();
                        
                        Mapping mapping = new Mapping();
                        mapping.setClassName(clazz.getName());
                        mapping.setMethodName(method.getName());
                        mapping.setHttpMethod(httpMethod);
                        mapping.setUrlPattern(url);
                        
                        // Clé = URL + méthode HTTP
                        String key = httpMethod + ":" + url;
                        mappings.put(key, mapping);
                    }
                    
                    // Vérifier si la méthode a l'annotation @GetMapping
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        GetMapping getMapping = method.getAnnotation(GetMapping.class);
                        String url = getMapping.url();
                        
                        Mapping mapping = new Mapping();
                        mapping.setClassName(clazz.getName());
                        mapping.setMethodName(method.getName());
                        mapping.setHttpMethod("GET");
                        mapping.setUrlPattern(url);
                        
                        // Clé = GET + URL
                        String key = "GET:" + url;
                        mappings.put(key, mapping);
                    }
                    
                    // Vérifier si la méthode a l'annotation @PostMapping
                    if (method.isAnnotationPresent(PostMapping.class)) {
                        PostMapping postMapping = method.getAnnotation(PostMapping.class);
                        String url = postMapping.url();
                        
                        Mapping mapping = new Mapping();
                        mapping.setClassName(clazz.getName());
                        mapping.setMethodName(method.getName());
                        mapping.setHttpMethod("POST");
                        mapping.setUrlPattern(url);
                        
                        // Clé = POST + URL
                        String key = "POST:" + url;
                        mappings.put(key, mapping);
                    }
                    
                    // NOUVEAU : Vérifier si la méthode a l'annotation @RestApi
                    if (method.isAnnotationPresent(RestApi.class)) {
                        RestApi restApi = method.getAnnotation(RestApi.class);
                        String url = restApi.url();
                        String httpMethod = restApi.method();
                        
                        Mapping mapping = new Mapping();
                        mapping.setClassName(clazz.getName());
                        mapping.setMethodName(method.getName());
                        mapping.setHttpMethod(httpMethod);
                        mapping.setUrlPattern(url);
                        mapping.setRestApi(true); // Marquer comme API REST
                        
                        // Clé = METHOD + URL
                        String key = httpMethod + ":" + url;
                        mappings.put(key, mapping);
                    }
                }
            }
        }
        
        return mappings;
    }
    
    private static List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        
        if (resource == null) {
            throw new Exception("Package non trouvé : " + packageName);
        }
        
        File directory = new File(resource.getFile());
        
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".class")) {
                        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                        classes.add(Class.forName(className));
                    }
                }
            }
        }
        
        return classes;
    }
}