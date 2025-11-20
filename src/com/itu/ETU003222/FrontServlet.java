package com.itu.ETU003222;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.HashMap;

public class FrontServlet extends HttpServlet {
    
    private HashMap<String, Mapping> routes = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            String packageName = getServletContext().getInitParameter("controllers-package");
            if (packageName == null) {
                packageName = "controllers"; // package par défaut
            }
            
            routes = Scanner.scanControllers(packageName);
            
            getServletContext().log("Framework initialisé avec " + routes.size() + " routes");
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controllers", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String resourcePath = requestURI.substring(contextPath.length());

        // 1. Vérifier si c'est une route mappée vers un controller
        if (routes.containsKey(resourcePath)) {
            Mapping mapping = routes.get(resourcePath);
            try {
                // Invoquer la méthode par réflexion
                Object result = Invoker.invoke(mapping);
                
                // Afficher les informations
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.println("<html><body>");
                out.println("<h3>Classe : " + mapping.getClassName() + "</h3>");
                out.println("<h3>Méthode : " + mapping.getMethodName() + "</h3>");
                
                // Si le résultat est un String, l'afficher aussi
                if (result instanceof String) {
                    out.println("<hr>");
                    out.println("<h4>Résultat :</h4>");
                    out.println(result);
                }
                
                out.println("</body></html>");
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'invocation de la méthode", e);
            }
            return;
        }

        // 2. Vérifier si c'est un fichier statique existant
        try {
            java.net.URL resource = getServletContext().getResource(resourcePath);
            if (resource != null) {
                RequestDispatcher defaultServlet = getServletContext().getNamedDispatcher("default");
                if (defaultServlet != null) {
                    defaultServlet.forward(request, response);
                    return;
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de la vérification de la ressource : " + resourcePath, e);
        }

        // 3. URL non trouvée - gérée par le framework
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>FrontServlet</h1>");
        out.println("<p>URL non trouvée, gérée par le framework : " + resourcePath + "</p>");
        out.println("</body></html>");
    }
}