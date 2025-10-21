package dev.ercan.jaxws.spring.binding;

import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;

public class SoapServiceBinding implements BeanNameAware {
  private String beanName;
  private String urlPattern;
  private WSEndpoint<?> endpoint;

  @Override
  public void setBeanName(@Nullable String name) {
    this.beanName = name;
  }

  public void create(ServletAdapterList owner) {
    String name = this.beanName;
    if (name == null) {
      name = this.urlPattern;
    }

    owner.createAdapter(name, this.urlPattern, this.endpoint);
  }

  public void setUrl(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  public void setService(WSEndpoint<?> endpoint) {
    this.endpoint = endpoint;
  }
}
