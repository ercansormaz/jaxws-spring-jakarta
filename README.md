# JAX-WS Spring Jakarta

[![Maven Central](https://img.shields.io/maven-central/v/dev.ercan/jaxws-spring-jakarta.svg)](https://search.maven.org/artifact/dev.ercan/jaxws-spring-jakarta)

**Jakarta-compatible SOAP library for Java 17+ and Spring Boot 4, designed for bottom-up development: generate WSDL from Java classes.**

---

## ⚠️ Disclaimer

This project is an **unofficial fork** of the original [org.jvnet.jax-ws-commons.spring:jaxws-spring](https://mvnrepository.com/artifact/org.jvnet.jax-ws-commons.spring/jaxws-spring) library.  
It has been modernized to be **Jakarta EE and Spring Boot 4 compatible**.  
Original code was licensed under **CDDL 1.1** and **GPL 2.0**.

Use this library for **educational purposes**, compatibility testing, or to support legacy SOAP endpoints with modern Java and Spring Boot.

---

## 💡 Features

- Bottom-up SOAP service support
- Java 17 compatible
- Spring Boot 4 compatible
- Jakarta namespace (`jakarta.xml.ws`) support
- Maven Central ready

---

## 📦 Maven Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.ercan</groupId>
    <artifactId>jaxws-spring-jakarta</artifactId>
    <version>4.0.4</version>
</dependency>
```

---

## 🔧 Example Usage

```java
import jakarta.jws.WebService;

@WebService
public class TestService {
  public String sayHello(String name) {
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

## 📄 License

This project is licensed under CDDL 1.1 (educational fork of the original library).
See [LICENSE](./LICENSE)   file for details.