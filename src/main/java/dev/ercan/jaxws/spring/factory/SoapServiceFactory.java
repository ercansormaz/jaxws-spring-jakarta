package dev.ercan.jaxws.spring.factory;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.databinding.MetadataReader;
import com.sun.xml.ws.api.pipe.TubelineAssembler;
import com.sun.xml.ws.api.pipe.TubelineAssemblerFactory;
import com.sun.xml.ws.api.server.BoundEndpoint;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.api.server.Module;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.server.EndpointFactory;
import com.sun.xml.ws.server.ServerRtException;
import com.sun.xml.ws.util.xml.XmlUtil;
import jakarta.servlet.ServletContext;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.handler.Handler;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;
import org.xml.sax.EntityResolver;

public class SoapServiceFactory implements FactoryBean<WSEndpoint>, ServletContextAware,
    InitializingBean {
  @NotNull
  private Class<?> implType;
  private Invoker invoker;
  private QName serviceName;
  private QName portName;
  private Container container;
  private SDDocumentSource primaryWsdl;
  private Object primaryWSDLResource;
  private Collection<? extends SDDocumentSource> metadata;
  private Collection<Object> metadataResources;
  private EntityResolver resolver;
  private Object assembler;
  private WSBinding binding;
  private BindingID bindingID;
  private List<WebServiceFeature> features;
  private List<Handler> handlers;
  private ServletContext servletContext;
  private WSEndpoint<?> endpoint;

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public void setImpl(Class implType) {
    this.implType = implType;
  }

  public void setBean(Object sei) {
    this.invoker = InstanceResolver.createSingleton(sei).createInvoker();
    if (this.implType == null) {
      this.implType = sei.getClass();
    }

  }

  public void setInvoker(Invoker invoker) {
    this.invoker = invoker;
  }

  public void setAssembler(Object assembler) {
    if (!(assembler instanceof TubelineAssembler) && !(assembler instanceof TubelineAssemblerFactory)) {
      throw new IllegalArgumentException("Invalid type for assembler " + assembler);
    } else {
      this.assembler = assembler;
    }
  }

  public void setServiceName(QName serviceName) {
    this.serviceName = serviceName;
  }

  public void setPortName(QName portName) {
    this.portName = portName;
  }

  public void setContainer(Container container) {
    this.container = container;
  }

  public void setBinding(WSBinding binding) {
    this.binding = binding;
  }

  public void setBindingID(String id) {
    this.bindingID = BindingID.parse(id);
  }

  public void setFeatures(List<WebServiceFeature> features) {
    this.features = features;
  }

  public void setHandlers(List<Handler> handlers) {
    this.handlers = handlers;
  }

  public void setPrimaryWsdl(Object primaryWsdl) throws IOException {
    this.primaryWSDLResource = primaryWsdl;
  }

  public void setMetadata(Collection<Object> metadata) {
    this.metadataResources = metadata;
  }

  public void setResolver(EntityResolver resolver) {
    this.resolver = resolver;
  }

  public WSEndpoint<?> getObject() throws Exception {
    if (this.endpoint == null) {
      if (this.binding == null) {
        if (this.bindingID == null) {
          this.bindingID = BindingID.parse(this.implType);
        }

        if (this.features != null && !this.features.isEmpty()) {
          this.binding = BindingImpl.create(this.bindingID, (WebServiceFeature[])this.features.toArray(new WebServiceFeature[this.features.size()]));
        } else {
          this.binding = BindingImpl.create(this.bindingID);
        }
      } else {
        if (this.bindingID != null) {
          throw new IllegalStateException("Both bindingID and binding are configured");
        }

        if (this.features != null) {
          throw new IllegalStateException("Both features and binding are configured");
        }
      }

      if (this.handlers != null) {
        List<Handler> chain = this.binding.getHandlerChain();
        chain.addAll(this.handlers);
        this.binding.setHandlerChain(chain);
      }

      if (this.primaryWsdl == null) {
        EndpointFactory.verifyImplementorClass(this.implType, (MetadataReader)null);
        String wsdlLocation = EndpointFactory.getWsdlLocation(this.implType);
        if (wsdlLocation != null) {
          this.primaryWsdl = this.convertStringToSource(wsdlLocation);
        }
      }

      EntityResolver resolver = this.resolver;
      if (resolver == null) {
        if (this.servletContext != null) {
          resolver = XmlUtil.createEntityResolver(this.servletContext.getResource("/WEB-INF/jax-ws-catalog.xml"));
        } else {
          resolver = XmlUtil.createEntityResolver(this.getClass().getClassLoader().getResource("/META-INF/jax-ws-catalog.xml"));
        }
      }

      this.endpoint = WSEndpoint.create(this.implType, false, this.invoker, this.serviceName, this.portName, new ContainerWrapper(), this.binding, this.primaryWsdl, this.metadata, resolver, true);
    }

    return this.endpoint;
  }

  public void afterPropertiesSet() throws Exception {
    if (this.primaryWSDLResource != null) {
      this.primaryWsdl = this.resolveSDDocumentSource(this.primaryWSDLResource);
    }

    if (this.metadataResources != null) {
      List<SDDocumentSource> tempList = new ArrayList<>(this.metadataResources.size());

      for(Object resource : this.metadataResources) {
        tempList.add(this.resolveSDDocumentSource(resource));
      }

      this.metadata = tempList;
    }

  }

  private SDDocumentSource resolveSDDocumentSource(Object resource) {
    SDDocumentSource source;
    if (resource instanceof String) {
      source = this.convertStringToSource((String)resource);
    } else if (resource instanceof URL) {
      source = SDDocumentSource.create((URL)resource);
    } else {
      if (!(resource instanceof SDDocumentSource)) {
        throw new IllegalArgumentException("Unknown type \"" + resource.getClass().getName() + "\" for resource " + resource);
      }

      source = (SDDocumentSource)resource;
    }

    return source;
  }

  private SDDocumentSource convertStringToSource(String resourceLocation) {
    URL url = null;
    if (this.servletContext != null) {
      try {
        url = this.servletContext.getResource(resourceLocation);
      } catch (MalformedURLException var5) {
      }
    }

    if (url == null) {
      ClassLoader cl = this.implType.getClassLoader();
      url = cl.getResource(resourceLocation);
    }

    if (url == null) {
      try {
        url = new URL(resourceLocation);
      } catch (MalformedURLException var4) {
      }
    }

    if (url == null) {
      throw new ServerRtException("cannot.load.wsdl", new Object[]{resourceLocation});
    } else {
      return SDDocumentSource.create(url);
    }
  }

  public boolean isSingleton() {
    return true;
  }

  public Class<WSEndpoint> getObjectType() {
    return WSEndpoint.class;
  }

  private class ContainerWrapper extends Container {
    private final Module module;

    private ContainerWrapper() {
      this.module = new Module() {
        private final List<BoundEndpoint> endpoints = new ArrayList();

        @NotNull
        public List<BoundEndpoint> getBoundEndpoints() {
          return this.endpoints;
        }
      };
    }

    public <T> T getSPI(Class<T> spiType) {
      if (spiType == TubelineAssemblerFactory.class) {
        if (SoapServiceFactory.this.assembler instanceof TubelineAssemblerFactory) {
          return (T)spiType.cast(SoapServiceFactory.this.assembler);
        }

        if (SoapServiceFactory.this.assembler instanceof TubelineAssembler) {
          return (T)spiType.cast(new TubelineAssemblerFactory() {
            public TubelineAssembler doCreate(BindingID bindingId) {
              return (TubelineAssembler) SoapServiceFactory.this.assembler;
            }
          });
        }
      }

      if (spiType == ServletContext.class) {
        return (T)spiType.cast(SoapServiceFactory.this.servletContext);
      } else {
        if (SoapServiceFactory.this.container != null) {
          T t = (T) SoapServiceFactory.this.container.getSPI(spiType);
          if (t != null) {
            return t;
          }
        }

        return (T)(spiType == Module.class ? spiType.cast(this.module) : null);
      }
    }
  }
}
