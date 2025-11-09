package com.itu.ETU003222;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;

import com.itu.ETU003222.annotation.UrlMapping;
import com.itu.ETU003222.annotation.AnnotationController;

public class Scanner {
    
    public static HashMap<String, Mapping> scanControllers(String packageName) throws Exception {
        HashMap<String, Mapping> routes = new HashMap<>();
        
        String path = packageName.replace(".", "/");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(path);

        if (url == null) {
            throw new Exception("Package introuvable : " + packageName);
        }

        File folder = new File(url.toURI());
        File[] files = folder.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> cls = Class.forName(className);

                    if (cls.isAnnotationPresent(AnnotationController.class)) {
                        scanMethods(cls, className, routes);
                    }
                }
            }
        }
        
        return routes;
    }
    
    private static void scanMethods(Class<?> cls, String className, HashMap<String, Mapping> routes) {
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(UrlMapping.class)) {
                UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                String url = annotation.value();
                routes.put(url, new Mapping(className, method.getName()));
            }
        }
    }
}