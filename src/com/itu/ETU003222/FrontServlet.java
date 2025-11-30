package com.itu.ETU003222;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.HashMap;
import java.util.Map;

import com.itu.ETU003222.model.ModelView;
import com.itu.ETU003222.model.Mapping;

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

        // 1. Vérifier si c'est une route exacte
        if (routes.containsKey(resourcePath)) {
            Mapping mapping = routes.get(resourcePath);
            handleMapping(mapping, request, response, null);
            return;
        }
        
        // 2. Vérifier si l'URL correspond à un pattern avec paramètres
        for (Map.Entry<String, Mapping> entry : routes.entrySet()) {
            Mapping mapping = entry.getValue();
            if (mapping.matches(resourcePath)) {
                // Extraire les paramètres
                Map<String, String> params = mapping.extractParams(resourcePath);
                handleMapping(mapping, request, response, params);
                return;
            }
        }

        // 3. Vérifier si c'est un fichier statique existant
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

        // 4. URL non trouvée
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>FrontServlet</h1>");
        out.println("<p>URL non trouvée, gérée par le framework : " + resourcePath + "</p>");
        out.println("</body></html>");
    }

    // Méthode pour gérer l'invocation du mapping
    private void handleMapping(Mapping mapping, HttpServletRequest request, 
                            HttpServletResponse response, Map<String, String> params) 
            throws ServletException, IOException {
        try {
            // Si des paramètres existent, les ajouter comme paramètres de requête
            if (params != null && !params.isEmpty()) {
                // Créer un wrapper pour ajouter les paramètres extraits
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getParameter(String name) {
                        // D'abord chercher dans les paramètres extraits de l'URL
                        if (params.containsKey(name)) {
                            return params.get(name);
                        }
                        // Sinon, utiliser les paramètres de requête normaux
                        return super.getParameter(name);
                    }
                };
                request = wrappedRequest;
                
                // Aussi les ajouter aux attributs pour compatibilité
                for (Map.Entry<String, String> param : params.entrySet()) {
                    request.setAttribute(param.getKey(), param.getValue());
                }
            }
            
            // Invoquer la méthode par réflexion en passant la requête
            Object result = Invoker.invoke(mapping, request);
            
            // Si le résultat est un ModelView, dispatcher vers la vue
            if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                String view = modelView.getView();
                
                // Transférer toutes les données du ModelView vers les attributs de la requête
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
                
                RequestDispatcher dispatcher = request.getRequestDispatcher(view);
                dispatcher.forward(request, response);
                return;
            }
            
            // Afficher les informations
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h3>Classe : " + mapping.getClassName() + "</h3>");
            out.println("<h3>Méthode : " + mapping.getMethodName() + "</h3>");
            
            // Afficher les paramètres extraits
            if (params != null && !params.isEmpty()) {
                out.println("<hr>");
                out.println("<h4>Paramètres extraits :</h4>");
                out.println("<ul>");
                for (Map.Entry<String, String> param : params.entrySet()) {
                    out.println("<li><strong>" + param.getKey() + "</strong> = " + param.getValue() + "</li>");
                }
                out.println("</ul>");
            }
            
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
    }
}