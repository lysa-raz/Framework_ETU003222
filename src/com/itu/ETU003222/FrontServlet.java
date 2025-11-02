package com.itu.ETU003222;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import com.itu.ETU003222.Mapping;
import com.itu.ETU003222.annotation.UrlMapping;
import com.itu.ETU003222.annotation.AnnotationController;

public class FrontServlet extends HttpServlet {

    private HashMap<String, Mapping> routes = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            scanControllers("controllers");
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controllers", e);
        }
    }

    private void scanControllers(String packageName) throws Exception {
        String path = packageName.replace(".", "/");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(path);

        if (url == null) return;

        File folder = new File(url.toURI());
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> cls = Class.forName(className);

                if (cls.isAnnotationPresent(AnnotationController.class)) {
                    for (Method m : cls.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(UrlMapping.class)) {
                            UrlMapping ann = m.getAnnotation(UrlMapping.class);
                            routes.put(ann.value(), new Mapping(className, m.getName()));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI().replace(request.getContextPath(), "");

        if (routes.containsKey(uri)) {
            Mapping map = routes.get(uri);
            response.getWriter().println("Classe trouvée : " + map.className);
            response.getWriter().println("Méthode trouvée : " + map.methodName);
            return;
        }

        response.getWriter().println("URL non trouvée : " + uri);
    }
}
