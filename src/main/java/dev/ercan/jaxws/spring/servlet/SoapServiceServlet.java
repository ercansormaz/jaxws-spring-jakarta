package dev.ercan.jaxws.spring.servlet;

import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;
import dev.ercan.jaxws.spring.binding.SoapServiceBinding;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * HTTP servlet for publishing and serving SOAP web services via JAX-WS.
 *
 * <p>This servlet integrates Spring web application context with JAX-WS to publish
 * SOAP web services. It collects all {@link SoapServiceBinding} beans from the Spring context and registers them with
 * the servlet adapter list. The servlet delegates HTTP requests (GET, POST) to the JAX-WS servlet
 * delegate for proper SOAP request handling.
 *
 * <p>Typical configuration in web.xml:
 * <pre>{@code
 * <servlet>
 *   <servlet-name>soap</servlet-name>
 *   <servlet-class>dev.ercan.jaxws.spring.servlet.SoapServiceServlet</servlet-class>
 *   <load-on-startup>1</load-on-startup>
 * </servlet>
 * <servlet-mapping>
 *   <servlet-name>soap</servlet-name>
 *   <url-pattern>/ws/*</url-pattern>
 * </servlet-mapping>
 * }</pre>
 *
 * <p>The servlet lifecycle:
 * <ol>
 *   <li>init() - Initializes servlet and discovers SoapServiceBinding beans</li>
 *   <li>doGet/doPost() - Handle HTTP requests</li>
 *   <li>destroy() - Cleans up resources</li>
 * </ol>
 *
 * @see SoapServiceBinding SOAP service binding configuration
 * @see WSServletDelegate JAX-WS servlet delegate for request handling
 */
public class SoapServiceServlet extends HttpServlet {

  /**
   * JAX-WS servlet delegate for handling SOAP requests.
   * AtomicReference is used to ensure thread-safe visibility and satisfy SonarQube S3077.
   */
  private final AtomicReference<WSServletDelegate> delegateReference = new AtomicReference<>();

  /**
   * Initializes the servlet and sets up SOAP web services. Retrieves all SoapServiceBinding beans from Spring context
   * and registers them with the servlet adapter list. Creates the WSServletDelegate for request handling.
   *
   * @param servletConfig the servlet configuration
   * @throws ServletException if initialization fails
   */
  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
    WebApplicationContext wac = getWebApplicationContext();
    ServletAdapterList l = new ServletAdapterList(getServletContext());

    Map<String, SoapServiceBinding> bindings = wac.getBeansOfType(SoapServiceBinding.class);
    if (!bindings.isEmpty()) {
      bindings.values().forEach(binding -> binding.create(l));
    }

    this.delegateReference.set(new WSServletDelegate(l, getServletContext()));
  }

  /**
   * Cleans up resources when the servlet is destroyed.
   */
  @Override
  public void destroy() {
    WSServletDelegate delegate = this.delegateReference.getAndSet(null);
    if (delegate != null) {
      delegate.destroy();
    }
  }

  /**
   * Handles HTTP POST requests (SOAP request-response messages). Delegates to WSServletDelegate for processing.
   *
   * @param request  the HTTP request
   * @param response the HTTP response
   * @throws ServletException if request handling fails
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    WSServletDelegate delegate = this.delegateReference.get();
    if (delegate != null) {
      delegate.doPost(request, response, getServletContext());
    }
  }

  /**
   * Handles HTTP GET requests (WSDL and metadata retrieval). Delegates to WSServletDelegate for processing.
   *
   * @param request  the HTTP request
   * @param response the HTTP response
   * @throws ServletException if request handling fails
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    WSServletDelegate delegate = this.delegateReference.get();
    if (delegate != null) {
      delegate.doGet(request, response, getServletContext());
    }
  }

  /**
   * Retrieves the Spring web application context for the servlet.
   *
   * @return the WebApplicationContext associated with this servlet
   * @throws IllegalStateException if the context is not found
   */
  private WebApplicationContext getWebApplicationContext() {
    return WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
  }
}
