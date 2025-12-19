package com.itu.ETU003222;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.MultipartConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import com.itu.ETU003222.model.ModelView;
import com.itu.ETU003222.model.Mapping;
import com.itu.ETU003222.model.ApiResponse;
import com.itu.ETU003222.model.FileUpload;
import com.google.gson.Gson;

@MultipartConfig // NOUVEAU : Active le support multipart/form-data pour upload
public class FrontServlet extends HttpServlet {
    
    private HashMap<String, Mapping> routes = new HashMap<>();
    private Gson gson = new Gson(); // instance Gson (déjà existant)

    @Override
    public void init() throws ServletException {
        try {
            String packageName = getServletContext().getInitParameter("controllers-package");
            if (packageName == null) {
                packageName = "controllers";
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
        String httpMethod = request.getMethod();

        String routeKey = httpMethod + ":" + resourcePath;

        // 1. Vérifier si c'est une route exacte avec la méthode HTTP
        if (routes.containsKey(routeKey)) {
            Mapping mapping = routes.get(routeKey);
            handleMapping(mapping, request, response, null);
            return;
        }
        
        // 2. Vérifier si l'URL correspond à un pattern avec paramètres
        for (Map.Entry<String, Mapping> entry : routes.entrySet()) {
            String key = entry.getKey();
            Mapping mapping = entry.getValue();
            
            String[] parts = key.split(":", 2);
            if (parts.length != 2) continue;
            
            String mappingMethod = parts[0];
            String mappingUrl = parts[1];
            
            if (!mappingMethod.equalsIgnoreCase(httpMethod)) continue;
            
            if (mapping.matches(resourcePath)) {
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

    private void handleMapping(Mapping mapping, HttpServletRequest request, 
                            HttpServletResponse response, Map<String, String> params) 
            throws ServletException, IOException {
        try {
            if (params != null && !params.isEmpty()) {
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getParameter(String name) {
                        if (params.containsKey(name)) {
                            return params.get(name);
                        }
                        return super.getParameter(name);
                    }
                };
                request = wrappedRequest;
                
                for (Map.Entry<String, String> param : params.entrySet()) {
                    request.setAttribute(param.getKey(), param.getValue());
                }
            }
            
            // NOUVEAU : Extraire les fichiers uploadés si présents
            Map<String, FileUpload> uploadedFiles = extractUploadedFiles(request);
            if (!uploadedFiles.isEmpty()) {
                request.setAttribute("uploadedFiles", uploadedFiles);
            }
            
            Object result = Invoker.invoke(mapping, request);
            
            // Vérifier si c'est une API REST
            if (mapping.isRestApi()) {
                handleRestApiResponse(result, response);
                return;
            }
            
            // Si le résultat est un ModelView, dispatcher vers la vue
            if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                String view = modelView.getView();
                
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
            
            if (params != null && !params.isEmpty()) {
                out.println("<hr>");
                out.println("<h4>Paramètres extraits :</h4>");
                out.println("<ul>");
                for (Map.Entry<String, String> param : params.entrySet()) {
                    out.println("<li><strong>" + param.getKey() + "</strong> = " + param.getValue() + "</li>");
                }
                out.println("</ul>");
            }
            
            if (result instanceof String) {
                out.println("<hr>");
                out.println("<h4>Résultat :</h4>");
                out.println(result);
            }
            
            out.println("</body></html>");
        } catch (Exception e) {
            if (mapping.isRestApi()) {
                handleRestApiError(e, response);
            } else {
                throw new ServletException("Erreur lors de l'invocation de la méthode", e);
            }
        }
    }
    
    // NOUVEAU : Extraire les fichiers uploadés depuis la requête
    private Map<String, FileUpload> extractUploadedFiles(HttpServletRequest request) throws IOException, ServletException {
        Map<String, FileUpload> files = new HashMap<>();
        
        // Vérifier si c'est une requête multipart/form-data
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            Collection<Part> parts = request.getParts();
            
            int fileIndex = 0; // NOUVEAU : Compteur pour les fichiers multiples
            
            for (Part part : parts) {
                String fileName = getFileName(part);
                
                // Si c'est un fichier (et non un champ texte)
                if (fileName != null && !fileName.isEmpty()) {
                    // Lire le contenu du fichier en bytes
                    InputStream fileContent = part.getInputStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    
                    byte[] data = new byte[1024];
                    int nRead;
                    while ((nRead = fileContent.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    
                    buffer.flush();
                    byte[] fileBytes = buffer.toByteArray();
                    
                    // Créer un objet FileUpload
                    FileUpload fileUpload = new FileUpload(
                        fileName,
                        part.getContentType(),
                        fileBytes,
                        part.getSize()
                    );
                    
                    // MODIFIÉ : Si plusieurs fichiers avec le même nom de champ, ajouter un index
                    String partName = part.getName();
                    String key = partName;
                    
                    // Si la clé existe déjà, ajouter un suffixe numérique
                    if (files.containsKey(key)) {
                        key = partName + "_" + fileIndex;
                    }
                    
                    files.put(key, fileUpload);
                    fileIndex++;
                }
            }
        }
        
        return files;
    }
    
    // NOUVEAU : Extraire le nom du fichier depuis Part
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String token : contentDisposition.split(";")) {
                if (token.trim().startsWith("filename")) {
                    return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }
    
    // Gérer les réponses REST API avec Gson
    private void handleRestApiResponse(Object result, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        if (result instanceof ApiResponse) {
            ApiResponse apiResponse = (ApiResponse) result;
            response.setStatus(apiResponse.getCode());
            out.print(gson.toJson(apiResponse));
        } else {
            ApiResponse apiResponse = ApiResponse.success(result);
            response.setStatus(200);
            out.print(gson.toJson(apiResponse));
        }
        
        out.flush();
    }
    
    // Gérer les erreurs REST API avec Gson
    private void handleRestApiError(Exception e, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        ApiResponse apiResponse = ApiResponse.serverError(e.getMessage());
        response.setStatus(500);
        out.print(gson.toJson(apiResponse));
        out.flush();
    }
}