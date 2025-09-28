package src.com.itu.ETU003222;

import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class FrontServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI();

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>FrontServlet</h1>");
        out.println("<p>URL non trouvée, gérée par le framework : " + path + "</p>");
        out.println("</body></html>");
    }

}
