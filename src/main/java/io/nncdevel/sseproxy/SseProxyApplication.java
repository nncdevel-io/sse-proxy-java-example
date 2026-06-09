package io.nncdevel.sseproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SseProxyApplication {

  public static void main(String[] args) {
    SpringApplication.run(SseProxyApplication.class, args);
  }
}
