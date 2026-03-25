package dev.ercan.jaxws.spring.factory;

import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSBinding;
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
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.handler.Handler;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;
import org.xml.sax.EntityResolver;

/**
 * Spring FactoryBean for creating and configuring JAX-WS {@link WSEndpoint} instances.
 * This factory integrates JAX-WS with the Spring lifecycle and dependency injection.
 */
public class SoapServiceFactory implements FactoryBean<WSEndpoint>, ServletContextAware, InitializingBean {

  /** The implementation class of the service. */
  private Class<?> implType;
  /** The invoker used to call methods on the service instance. */
  private Invoker invoker;
  /** The cached endpoint instance. */
  private WSEndpoint<?> endpoint;
  /** The explicit WSBinding to use. */
  private WSBinding binding;
  /** The binding ID (e.g., SOAP 1.1/1.2). */
  private BindingID bindingID;
  /** List of WebServiceFeatures to apply. */
  private List<WebServiceFeature> features;
  /** List of SOAP handlers for the handler chain. */
  private List<Handler> handlers;
  /** The resolved primary WSDL source. */
  private SDDocumentSource primaryWsdl;
  /** The raw primary WSDL resource (String, URL, or Source). */
  private Object primaryWSDLResource;
  /** The servlet context for resource resolution. */
  private ServletContext servletContext;
  /** Custom entity resolver for XML catalogs. */
  private EntityResolver resolver;
  /** The service QName. */
  private QName serviceName;
  /** The port QName. */
  private QName portName;
  /** Resolved additional metadata sources. */
  private Collection<? extends SDDocumentSource> metadata;
  /** Raw metadata resources. */
  private Collection<Object> metadataResources;
  /** Custom tubeline assembler. */
  private Object assembler;
  /** Parent container for SPI lookups. */
  private Container container;

  /**
   * Default constructor.
   */
  public SoapServiceFactory() {
  }

  /**
   * Sets the invoker for method dispatching.
   * @param invoker the invoker instance
   */
  public void setInvoker(Invoker invoker) {
    this.invoker = invoker;
  }

  /**
   * Sets the custom tubeline assembler or factory.
   * @param assembler a {@link TubelineAssembler} or {@link TubelineAssemblerFactory}
   */
  public void setAssembler(Object assembler) {
    if (!(assembler instanceof TubelineAssembler) && !(assembler instanceof TubelineAssemblerFactory)) {
      throw new IllegalArgumentException("Invalid type for assembler " + assembler);
    } else {
      this.assembler = assembler;
    }
  }

  /**
   * Sets the service qualified name.
   * @param serviceName the service name
   */
  public void setServiceName(QName serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Sets the port qualified name.
   * @param portName the port name
   */
  public void setPortName(QName portName) {
    this.portName = portName;
  }

  /**
   * Sets the parent container.
   * @param container the container
   */
  public void setContainer(Container container) {
    this.container = container;
  }

  /**
   * Sets the web service binding instance.
   * @param binding the binding
   */
  public void setBinding(WSBinding binding) {
    this.binding = binding;
  }

  /**
   * Sets the binding ID string.
   * @param id the binding ID
   */
  public void setBindingID(String id) {
    this.bindingID = BindingID.parse(id);
  }

  /**
   * Sets the list of web service features.
   * @param features the features
   */
  public void setFeatures(List<WebServiceFeature> features) {
    this.features = features;
  }

  /**
   * Sets the list of SOAP handlers.
   * @param handlers the handlers
   */
  public void setHandlers(List<Handler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Sets the primary WSDL resource path or object.
   * @param primaryWSDLResource the WSDL resource
   */
  public void setPrimaryWSDLResource(Object primaryWSDLResource) {
    this.primaryWSDLResource = primaryWSDLResource;
  }

  /**
   * Sets the additional metadata resources.
   * @param metadata the metadata collection
   */
  public void setMetadata(Collection<Object> metadata) {
    this.metadataResources = metadata;
  }

  /**
   * Sets the entity resolver for XML catalog resolution.
   * @param resolver the entity resolver
   */
  public void setResolver(EntityResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Creates or returns the cached WSEndpoint.
   * @return the endpoint
   * @throws Exception if creation fails
   */
  @Override
  public WSEndpoint<?> getObject() throws Exception {
    if (this.endpoint == null) {
      if (this.binding == null) {
        if (this.bindingID == null) {
          this.bindingID = BindingID.parse(this.implType);
        }

        if (CollectionUtils.isEmpty(this.features)) {
          this.binding = BindingImpl.create(this.bindingID);
        } else {
          this.binding = BindingImpl.create(this.bindingID, this.features.toArray(new WebServiceFeature[0]));
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
        EndpointFactory.verifyImplementorClass(this.implType, null);
        String wsdlLocation = EndpointFactory.getWsdlLocation(this.implType);
        if (wsdlLocation != null) {
          this.primaryWsdl = this.convertStringToSource(wsdlLocation);
        }
      }

      EntityResolver entityResolver = this.resolver;
      if (entityResolver == null) {
        if (this.servletContext != null) {
          entityResolver = XmlUtil.createEntityResolver(this.servletContext.getResource("/WEB-INF/jax-ws-catalog.xml"));
        } else {
          entityResolver = XmlUtil.createEntityResolver(this.getClass().getClassLoader().getResource("/META-INF/jax-ws-catalog.xml"));
        }
      }

      this.endpoint = WSEndpoint.create(this.implType, false, this.invoker, this.serviceName, this.portName,
          new ContainerWrapper(), this.binding, this.primaryWsdl, this.metadata, entityResolver, true);
    }

    return this.endpoint;
  }

  /**
   * Returns the type of object produced by this factory.
   * @return {@code WSEndpoint.class}
   */
  @Override
  public Class<?> getObjectType() {
    return WSEndpoint.class;
  }

  /**
   * Resolves resources after properties are set by Spring.
   */
  @Override
  public void afterPropertiesSet() {
    if (this.primaryWSDLResource != null) {
      this.primaryWsdl = this.resolveSDDocumentSource(this.primaryWSDLResource);
    }

    if (this.metadataResources != null) {
      List<SDDocumentSource> tempList = new ArrayList<>(this.metadataResources.size());

      for (Object resource : this.metadataResources) {
        tempList.add(this.resolveSDDocumentSource(resource));
      }

      this.metadata = tempList;
    }
  }

  /**
   * Sets the servlet context.
   * @param servletContext the context
   */
  @Override
  public void setServletContext(@Nonnull ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * Sets a Spring bean as the service instance (singleton invoker).
   * @param sei the service instance
   */
  public void setBean(Object sei) {
    this.invoker = InstanceResolver.createSingleton(sei).createInvoker();
    if (this.implType == null) {
      this.implType = sei.getClass();
    }
  }

  /**
   * Converts a string location to an {@link SDDocumentSource}.
   * @param resourceLocation the location path
   * @return the source
   */
  private SDDocumentSource convertStringToSource(String resourceLocation) {
    URL url = null;
    if (this.servletContext != null) {
      try {
        url = this.servletContext.getResource(resourceLocation);
      } catch (MalformedURLException ignored) {
      }
    }

    if (url == null) {
      ClassLoader cl = this.implType.getClassLoader();
      url = cl.getResource(resourceLocation);
    }

    if (url == null) {
      try {
        url = URI.create(resourceLocation).toURL();
      } catch (MalformedURLException | IllegalArgumentException ignored) {
      }
    }

    if (url == null) {
      throw new ServerRtException("cannot.load.wsdl", resourceLocation);
    } else {
      return SDDocumentSource.create(url);
    }
  }

  /**
   * Resolves a generic resource object into an {@link SDDocumentSource}.
   * @param resource the resource (String, URL, or Source)
   * @return the resolved source
   */
  private SDDocumentSource resolveSDDocumentSource(Object resource) {
    if (resource instanceof String resourceLocation) {
      return this.convertStringToSource(resourceLocation);
    } else if (resource instanceof URL url) {
      return SDDocumentSource.create(url);
    } else if (resource instanceof SDDocumentSource sdDocumentSource) {
      return sdDocumentSource;
    }
    throw new IllegalArgumentException("Unknown type \"" + resource.getClass().getName() + "\" for resource " + resource);
  }

  /**
   * Internal JAX-WS Container wrapper for Spring integration.
   */
  private class ContainerWrapper extends Container {

    /** The associated module. */
    private final Module module;

    /**
     * Creates a new wrapper instance.
     */
    private ContainerWrapper() {
      this.module = new Module() {
        private final List<BoundEndpoint> endpoints = new ArrayList<>();

        @Override
        @Nonnull
        public List<BoundEndpoint> getBoundEndpoints() {
          return this.endpoints;
        }
      };
    }

    /**
     * Resolves SPI implementations.
     * @param spiType the SPI class
     * @param <T> the type
     * @return the implementation or null
     */
    @Override
    public <T> T getSPI(Class<T> spiType) {
      if (spiType == TubelineAssemblerFactory.class) {
        if (SoapServiceFactory.this.assembler instanceof TubelineAssemblerFactory) {
          return spiType.cast(SoapServiceFactory.this.assembler);
        }

        if (SoapServiceFactory.this.assembler instanceof TubelineAssembler ta) {
          return spiType.cast(new TubelineAssemblerFactory() {
            @Override
            public TubelineAssembler doCreate(BindingID bindingId) {
              return ta;
            }
          });
        }
      }

      if (spiType == ServletContext.class) {
        return spiType.cast(SoapServiceFactory.this.servletContext);
      } else {
        if (SoapServiceFactory.this.container != null) {
          T t = SoapServiceFactory.this.container.getSPI(spiType);
          if (t != null) {
            return t;
          }
        }

        return (spiType == Module.class ? spiType.cast(this.module) : null);
      }
    }
  }
}
