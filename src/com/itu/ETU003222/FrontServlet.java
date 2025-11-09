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
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h1>Route trouvée</h1>");
            out.println("<p>Classe : " + mapping.getClassName() + "</p>");
            out.println("<p>Méthode : " + mapping.getMethodName() + "</p>");
            out.println("</body></html>");
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