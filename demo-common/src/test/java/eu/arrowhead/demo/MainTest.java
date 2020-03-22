package eu.arrowhead.demo;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.CertificateCreationRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.ssl.SSLException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = CommonConstants.BASE_PACKAGE)
public class MainTest {

  @Autowired
  private ArrowheadHandler onboardingHandler;

  @Value("${server.name}")
  private String clientName;

  public static void main(String[] args)
      throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, SSLException
      , IOException {
    final ConfigurableApplicationContext ctx = SpringApplication.run(MainTest.class);
    final MainTest test = ctx.getBean(MainTest.class);
    test.execute();
  }

  private void execute()
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SSLException,
             InvalidKeySpecException {
    final CertificateCreationRequestDTO creationRequest = new CertificateCreationRequestDTO(clientName);
    final OnboardingWithNameRequestDTO onboardingRequest = new OnboardingWithNameRequestDTO(creationRequest);
    onboardingHandler.performOnboarding(onboardingRequest);
  }
}
