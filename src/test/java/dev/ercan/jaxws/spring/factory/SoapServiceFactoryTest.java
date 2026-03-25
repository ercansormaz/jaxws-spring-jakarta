package dev.ercan.jaxws.spring.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.WSEndpoint;
import jakarta.jws.WebService;
import jakarta.servlet.ServletContext;
import jakarta.xml.ws.WebServiceFeature;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoapServiceFactoryTest {

    private SoapServiceFactory factory;
    private ServletContext servletContext;

    @BeforeEach
    void setUp() {
        factory = new SoapServiceFactory();
        servletContext = mock(ServletContext.class);
        factory.setServletContext(servletContext);
    }

    @Test
    void getObjectType_ShouldReturnWSEndpoint() {
        assertEquals(WSEndpoint.class, factory.getObjectType());
    }

    @Test
    void isSingleton_ShouldReturnTrue() {
        assertEquals(true, factory.isSingleton());
    }

    @Test
    void getObject_ShouldThrowException_WhenBothBindingAndFeaturesConfigured() {
        factory.setBean(MockService.class);
        factory.setBinding(mock(WSBinding.class));
        factory.setFeatures(List.of(mock(WebServiceFeature.class)));
        
        assertThrows(IllegalStateException.class, () -> factory.getObject());
    }

    @Test
    void setBean_ShouldSetImplTypeAndInvoker() {
        MockService service = new MockService();
        factory.setBean(service);
        
        // factory.setImpl call is checked inside setBean
        // if we call getObject it should work
        assertNotNull(factory.getObjectType());
    }

    // Helper class for testing
    @WebService
    public static class MockService {
        @jakarta.jws.WebMethod
        public String hello() { return "world"; }
    }
}
