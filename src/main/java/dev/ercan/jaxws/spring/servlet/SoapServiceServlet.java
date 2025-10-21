package dev.ercan.jaxws.spring.servlet;

import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;
import dev.ercan.jaxws.spring.binding.SoapServiceBinding;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SoapServiceServlet extends HttpServlet {

  private WSServletDelegate delegate;

  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
    WebApplicationContext wac = getWebApplicationContext();
    ServletAdapterList l = new ServletAdapterList(this.getServletContext());
    wac.getBeansOfType(SoapServiceBinding.class).values().forEach(val -> val.create(l));
    this.delegate = new WSServletDelegate(l, this.getServletContext());
  }

  public void destroy() {
    WebApplicationContext wac = getWebApplicationContext();
    if (wac instanceof ConfigurableApplicationContext) {
      ((ConfigurableApplicationContext) wac).close();
    }

    this.delegate.destroy();
    this.delegate = null;
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException {
    this.delegate.doPost(request, response, this.getServletContext());
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException {
    this.delegate.doGet(request, response, this.getServletContext());
  }

  protected void doPut(HttpServletRequest request, HttpServletResponse response)
      throws ServletException {
    this.delegate.doPut(request, response, this.getServletContext());
  }

  protected void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws ServletException {
    this.delegate.doDelete(request, response, this.getServletContext());
  }

  private WebApplicationContext getWebApplicationContext() {
    return WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext());
  }
}
