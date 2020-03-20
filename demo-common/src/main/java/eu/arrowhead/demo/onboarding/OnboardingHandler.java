package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.core.CoreSystemService;
import eu.arrowhead.common.dto.shared.DeviceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceEndpoint;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.demo.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OnboardingHandler {

  private final Logger logger = LogManager.getLogger(OnboardingHandler.class);

  private final HttpHandler httpService;
  private final SSLHandler sslHandler;
  private final String onboardingHost;

  private ServiceEndpoint deviceRegistry;
  private ServiceEndpoint systemRegistry;
  private ServiceEndpoint serviceRegistry;
  private ServiceEndpoint orchestrationService;

  @Autowired
  public OnboardingHandler(final HttpHandler httpService, SSLHandler sslHandler,
                           @Value("${onboarding.host}") final String onboardingHost) {
    this.httpService = httpService;
    this.sslHandler = sslHandler;
    this.onboardingHost = onboardingHost;
  }

  public void performOnboarding(final OnboardingWithNameRequestDTO onboardingRequest)
      throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, KeyStoreException, IOException,
             SSLException {

    logger.info("Starting onboarding for {}", onboardingRequest);
    httpService.setInsecure();

    final UriComponents onboardingUri = Utilities
        .createURI(CommonConstants.HTTPS, onboardingHost, CoreSystem.ONBOARDING_CONTROLLER.getDefaultPort(),
                   CoreSystemService.ONBOARDING_WITH_SHARED_SECRET_AND_NAME.getServiceUri());

    final ResponseEntity<OnboardingWithNameResponseDTO> httpResponse = httpService
        .sendRequest(onboardingUri, HttpMethod.POST, OnboardingWithNameResponseDTO.class, onboardingRequest);

    final OnboardingWithNameResponseDTO responseDTO = httpResponse.getBody();
    assert responseDTO != null;

    deviceRegistry = responseDTO.getDeviceRegistry();
    systemRegistry = responseDTO.getSystemRegistry();
    serviceRegistry = responseDTO.getServiceRegistry();
    orchestrationService = responseDTO.getOrchestrationService();

    logger.info("Saving certificates ...");
    sslHandler.adaptSSLContext(onboardingRequest.getCreationRequestDTO().getCommonName(),
                               responseDTO.getCertificateType(),
                               responseDTO.getOnboardingCertificate(),
                               responseDTO.getIntermediateCertificate(),
                               responseDTO.getRootCertificate());

    httpService.setSecure();
  }

  public void registerDevice(final DeviceRegistryRequestDTO registryRequestDTO) {
    register(registryRequestDTO, DeviceRegistryResponseDTO.class, deviceRegistry.getUri());
  }

  public void registerSystem(final SystemRegistryResponseDTO registryRequestDTO) {
    register(registryRequestDTO, SystemRegistryResponseDTO.class, systemRegistry.getUri());
  }

  public void registerService(final ServiceRegistryResponseDTO registryRequestDTO) {
    register(registryRequestDTO, ServiceRegistryResponseDTO.class, serviceRegistry.getUri());
  }

  private <REQ, RES> RES register(final REQ registryRequest, final Class<RES> responseCls, final URI uri) {
    final UriComponents registryUri = UriComponentsBuilder.fromUri(uri).build();
    final ResponseEntity<RES> responseEntity = httpService
        .sendRequest(registryUri, HttpMethod.POST, responseCls, registryRequest);
    return responseEntity.getBody();
  }

}
