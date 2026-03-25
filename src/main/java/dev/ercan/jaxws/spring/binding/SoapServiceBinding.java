package dev.ercan.jaxws.spring.binding;

import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import java.util.Objects;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Represents the configuration for binding a JAX-WS endpoint to a specific URL pattern.
 * This class is intended to be managed as a Spring bean.
 */
public class SoapServiceBinding implements BeanNameAware {

  private String beanName;
  private String urlPattern;
  private WSEndpoint<?> endpoint;

  /**
   * Default constructor for Spring bean initialization.
   */
  public SoapServiceBinding() {
  }

  /**
   * Convenience constructor for programmatic setup.
   *
   * @param urlPattern the URL pattern (e.g., "/example-service")
   * @param endpoint   the JAX-WS endpoint implementation
   */
  public SoapServiceBinding(String urlPattern, WSEndpoint<?> endpoint) {
    this.urlPattern = urlPattern;
    this.endpoint = endpoint;
  }

  @Override
  public void setBeanName(@Nullable String name) {
    this.beanName = name;
  }

  /**
   * Registers this binding to the provided JAX-WS adapter list.
   *
   * @param adapterList the adapter list to register with
   * @throws IllegalStateException if urlPattern or endpoint is not configured
   */
  public void create(@NonNull ServletAdapterList adapterList) {
    Objects.requireNonNull(adapterList, "adapterList must not be null");

    if (urlPattern == null || endpoint == null) {
      throw new IllegalStateException(String.format(
          "SoapServiceBinding [%s] is incomplete: both urlPattern and endpoint are required.",
          beanName != null ? beanName : "unnamed"));
    }

    // Use beanName as the unique identifier if available, fallback to urlPattern
    String name = (beanName != null) ? beanName : urlPattern;
    adapterList.createAdapter(name, urlPattern, endpoint);
  }

  /**
   * Sets the URL pattern (e.g., "/myService").
   *
   * @param urlPattern the URL pattern for the endpoint
   */
  public void setUrl(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  /**
   * Sets the JAX-WS endpoint.
   *
   * @param endpoint the JAX-WS web service endpoint
   */
  public void setService(WSEndpoint<?> endpoint) {
    this.endpoint = endpoint;
  }

  /**
   * Returns the configured URL pattern.
   *
   * @return the URL pattern
   */
  public String getUrlPattern() {
    return urlPattern;
  }

  /**
   * Returns the configured JAX-WS endpoint.
   *
   * @return the JAX-WS endpoint
   */
  public WSEndpoint<?> getEndpoint() {
    return endpoint;
  }
}
