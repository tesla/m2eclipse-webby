package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String greeting;
    try (InputStream is = getServletContext().getResourceAsStream("/WEB-INF/info/greeting.txt")) {
      greeting = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8).trim() : "no greeting";
    }
    resp.setContentType("text/plain");
    resp.setCharacterEncoding("UTF-8");
    resp.getWriter().print(greeting + " (" + System.getProperty("webby.sample.property", "unset") + ")");
  }

}
