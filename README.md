# JAX-WS Spring Jakarta

[![Maven Central](https://img.shields.io/maven-central/v/dev.ercan/jaxws-spring-jakarta.svg)](https://search.maven.org/artifact/dev.ercan/jaxws-spring-jakarta)

**Jakarta-compatible SOAP library for Java 17+ and Spring Boot 3, designed for bottom-up development: generate WSDL from Java classes.**

---

## 💡 Features

- Bottom-up SOAP service support
- Java 17+ compatible
- Spring Boot 3 compatible
- Jakarta namespace (`jakarta.xml.ws`) support
- Maven Central ready

---

## 📦 Maven Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.ercan</groupId>
    <artifactId>jaxws-spring-jakarta</artifactId>
    <version>3.5.13</version>
</dependency>
```

---

## 🔧 Example Usage

```java
import jakarta.jws.WebService;

@WebService
public class TestService {

  @WebResult(name = "response")
  public String sayHello(@WebParam(name = "request") String name) {
    return "Hello " + name;
  }
}
```

```java
import dev.ercan.jaxws.spring.binding.SoapServiceBinding;
import dev.ercan.jaxws.spring.factory.SoapServiceFactory;
import dev.ercan.jaxws.spring.servlet.SoapServiceServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SoapServiceConfig {

  @Bean
  public ServletRegistrationBean<SoapServiceServlet> SoapServiceServlet() {
    SoapServiceServlet soapWsServlet = new SoapServiceServlet();
    ServletRegistrationBean<SoapServiceServlet> bean = new ServletRegistrationBean<>(soapWsServlet);
    bean.setLoadOnStartup(1);
    return bean;
  }

  @Bean
  public TestService testService(){
    return new TestService();
  }

  @Bean
  public SoapServiceBinding testServiceBinding(TestService testService) throws Exception {
    SoapServiceFactory springService = new SoapServiceFactory();
    springService.setBean(testService);

    SoapServiceBinding springBinding = new SoapServiceBinding();
    springBinding.setUrl("/SOAP/TestService");
    springBinding.setService(springService.getObject());

    return springBinding;
  }
}
```

> Access the WSDL at:
> http://localhost:8080/SOAP/TestService?wsdl

---

## 🤝 Contributing
Contributions are welcome! Feel free to fork the repo, submit pull requests or open issues.

--- 

## 📄 License

This project is licensed under the MIT License.

