package dev.ercan.jaxws.spring.binding;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoapServiceBindingTest {

    private SoapServiceBinding binding;
    private ServletAdapterList adapterList;
    private WSEndpoint<?> endpoint;

    @BeforeEach
    void setUp() {
        binding = new SoapServiceBinding();
        adapterList = mock(ServletAdapterList.class);
        endpoint = mock(WSEndpoint.class);
    }

    @Test
    void create_ShouldRegisterWithAdapterList_WhenConfiguredCorrectly() {
        binding.setBeanName("testBean");
        binding.setUrl("/ws/test");
        binding.setService(endpoint);

        binding.create(adapterList);

        verify(adapterList).createAdapter(eq("testBean"), eq("/ws/test"), eq(endpoint));
    }

    @Test
    void create_ShouldUseUrlPattern_WhenBeanNameIsNull() {
        binding.setUrl("/ws/test");
        binding.setService(endpoint);

        binding.create(adapterList);

        verify(adapterList).createAdapter(eq("/ws/test"), eq("/ws/test"), eq(endpoint));
    }

    @Test
    void create_ShouldThrowIllegalStateException_WhenUrlPatternIsNull() {
        binding.setService(endpoint);

        assertThrows(IllegalStateException.class, () -> binding.create(adapterList));
    }

    @Test
    void create_ShouldThrowIllegalStateException_WhenEndpointIsNull() {
        binding.setUrl("/ws/test");

        assertThrows(IllegalStateException.class, () -> binding.create(adapterList));
    }
}
