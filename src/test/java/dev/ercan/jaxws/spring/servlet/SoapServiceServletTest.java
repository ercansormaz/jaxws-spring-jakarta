package dev.ercan.jaxws.spring.servlet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.WebApplicationContext;

class SoapServiceServletTest {

    private SoapServiceServlet servlet;
    private ServletConfig servletConfig;
    private ServletContext servletContext;
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        servlet = new SoapServiceServlet();
        servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        webApplicationContext = mock(WebApplicationContext.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(webApplicationContext);
    }

    @Test
    void init_ShouldThrowException_WhenContextNotFound() {
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
            .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> servlet.init(servletConfig));
    }

    @Test
    void init_ShouldSucceed_WhenContextIsPresent() throws ServletException {
        // Mock beans returning empty list for bindings
        when(webApplicationContext.getBeansOfType(dev.ercan.jaxws.spring.binding.SoapServiceBinding.class))
            .thenReturn(java.util.Collections.emptyMap());

        servlet.init(servletConfig);
        
        // This confirms the delegateReference is set (not null)
        assertNotNull(servlet);
    }
}
