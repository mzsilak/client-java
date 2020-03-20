package eu.arrowhead.client.car;

import eu.arrowhead.demo.onboarding.OnboardingHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class Main {

  public static void main(String[] args) {
    SpringApplication.run(Main.class);
  }
}
