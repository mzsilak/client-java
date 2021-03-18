package eu.arrowhead.client.car;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateCreationRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.dto.RegisterRequestDTO;
import eu.arrowhead.demo.dto.RegisterResponseDTO;
import eu.arrowhead.demo.dto.UnregisterRequestDTO;
import eu.arrowhead.demo.dto.UnregisterResponseDTO;
import eu.arrowhead.demo.events.OffboardingFinishedEvent;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.grovepi.GroveButtonObserver;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.onboarding.HttpClient;
import eu.arrowhead.demo.utils.IpUtilities;
import eu.arrowhead.demo.web.HttpServer;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;

@Service
public class ElectricCarApplication {

  private final Logger logger = LogManager.getLogger();
  private final AtomicBoolean onboarded = new AtomicBoolean(false);
  private final ApplicationEventPublisher applicationEventPublisher;
  private final HttpServer httpServer;

  private final ArrowheadHandler onboardingHandler;
  private final HttpClient httpClient;
  private final String commonName;
  private final String rfid;

  private final ControllableLed green;
  private final ControllableLed red;

  private final String ipAddress;
  private final String macAddress;
  private final String validity;
  private final int port;

  @Autowired
  public ElectricCarApplication(final ApplicationEventPublisher applicationEventPublisher, final HttpServer httpServer,
                                final ArrowheadHandler onboardingHandler, final HttpClient httpClient,
                                @Value("${server.name}") final String commonName,
                                @Value("${server.rfid}") final String rfid,
                                @Qualifier("greenControl") final ControllableLed green,
                                @Qualifier("redControl") final ControllableLed red,
                                final GroveButtonObserver buttonObserver) throws IOException {
    this.applicationEventPublisher = applicationEventPublisher;
    this.httpServer = httpServer;
    this.onboardingHandler = onboardingHandler;
    this.httpClient = httpClient;
    this.commonName = commonName;
    this.rfid = rfid;
    this.green = green;
    this.red = red;

    httpServer.configureName(commonName);
    buttonObserver.setListener(this::toggleOnboarding);

    ipAddress = IpUtilities.getAddressString();
    macAddress = IpUtilities.getMacAddress(ipAddress);
    validity = Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now().plusDays(1));
    port = httpServer.getPort();

    buttonObserver.start();
    logger.info("Listening to button");
  }

  public void toggleOnboarding() {
    logger.info("Button trigger detected");
    if (onboarded.get()) {
      performOffboarding();
    } else {
      performOnboarding();
    }
  }

  @PreDestroy
  public void performOffboarding() {
    try {
      httpServer.stop();
      green.blink();
      red.blink();

      logger.info("Unregistering myself ...");

      try {
        if (onboarded.get()) {
          final UriComponents unregisterUri = findUri(Constants.SERVICE_STATION_UNREGISTER);
          httpClient
              .sendRequest(unregisterUri, HttpMethod.POST, UnregisterResponseDTO.class, new UnregisterRequestDTO(rfid));
        }
      } catch (final Exception e) {
        logger.debug("Unable to unregister rfid: {}", e.getMessage());
      }

      onboardingHandler.unregisterService(Constants.SERVICE_CAR_RFID, commonName, ipAddress, port);
      onboardingHandler.unregisterSystem(commonName, ipAddress, port);
      onboardingHandler.unregisterDevice(commonName, macAddress);

    } catch (final Exception e) {
      logger.warn("Offboarding issue: {}: {}", e.getClass().getSimpleName(), e.getMessage());
    } finally {
      onboarded.set(false);
      stopStatusLeds();
    }
  }

  public void performOnboarding() {
    try {
      green.blink();
      red.blink();

      logger.info("Start onboarding ...");
      final var creationRequest = new CertificateCreationRequestDTO(commonName);
      onboardingHandler.onboard(new OnboardingWithNameRequestDTO(creationRequest));

      final String authInfo = onboardingHandler.getAuthInfo();

      final var deviceRequest = getDeviceRegistryRequest(ipAddress, macAddress, validity, authInfo, creationRequest);
      final var systemRequest = getSystemRegistryRequest(ipAddress, macAddress, port, validity, rfid, authInfo,
                                                         creationRequest);
      final var serviceRequest = getServiceRegistryRequest(ipAddress, port, validity, rfid, authInfo,
                                                           Constants.OP_CAR_RFID_URI, Constants.SERVICE_CAR_RFID);
      onboardingHandler.registerDevice(deviceRequest);
      onboardingHandler.registerSystem(systemRequest);
      onboardingHandler.registerService(serviceRequest);

      final UriComponents registerUri = findUri(Constants.SERVICE_STATION_REGISTER);
      httpClient.sendRequest(registerUri, HttpMethod.POST, RegisterResponseDTO.class, new RegisterRequestDTO(rfid));

      red.turnOff();
      onboarded.set(true);
    } catch (final Exception e) {
      logger.error("Onboarding issue: {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
      performOffboarding();
    }

    try {
      applicationEventPublisher.publishEvent(new OnboardingFinishedEvent(this));
    } catch (final Exception e) {
      logger.warn("Publishing issue: {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
      try {
        startWebServer();
      } catch (IOException ex) {
        logger.error("Unable to start webserver: {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
      }
    }
  }

  @EventListener(OnboardingFinishedEvent.class)
  public void startWebServer() throws IOException {
    httpServer.init();
    httpServer.start();
    green.turnOn();
    logger.info("Status led turned on");
  }

  @EventListener(OffboardingFinishedEvent.class)
  public void stopStatusLeds() {
    green.turnOff();
    red.turnOff();
    logger.info("Status led(s) turned off");
  }

  private DeviceRegistryOnboardingWithNameRequestDTO getDeviceRegistryRequest(String ipAddress, String macAddress,
                                                                              String validity, String authInfo,
                                                                              CertificateCreationRequestDTO creationRequest) {

    return new DeviceRegistryOnboardingWithNameRequestDTO(getDeviceRequest(ipAddress, macAddress, authInfo), validity,
                                                          null, null, creationRequest);
  }

  private SystemRegistryOnboardingWithNameRequestDTO getSystemRegistryRequest(String ipAddress, String macAddress,
                                                                              final int port, String validity,
                                                                              String rfid, String authInfo,
                                                                              CertificateCreationRequestDTO creationRequest) {
    final Map<String, String> metadata = new HashMap<>();
    metadata.put("rfid", rfid);
    return new SystemRegistryOnboardingWithNameRequestDTO(getSystemRequest(ipAddress, port, authInfo),
                                                          getDeviceRequest(ipAddress, macAddress, authInfo), validity,
                                                          metadata, null, creationRequest);
  }

  private ServiceRegistryRequestDTO getServiceRegistryRequest(String ipAddress, final int port, final String validity,
                                                              final String rfid, final String authInfo,
                                                              final String uriPostfix, final String serviceDef) {
    final ServiceRegistryRequestDTO requestDTO = new ServiceRegistryRequestDTO();
    requestDTO.setSecure(ServiceSecurityType.CERTIFICATE.name());
    requestDTO.setServiceUri(Constants.CAR_CONTROLLER_PATH + uriPostfix);
    requestDTO.setInterfaces(List.of(CommonConstants.HTTP_SECURE_JSON));
    requestDTO.setEndOfValidity(validity);
    requestDTO.setProviderSystem(getSystemRequest(ipAddress, port, authInfo));
    requestDTO.setServiceDefinition(serviceDef);
    requestDTO.setMetadata(Map.of("rfid", rfid));
    return requestDTO;
  }

  private UriComponents findUri(final String serviceDef) {
    final ServiceQueryFormDTO serviceQueryFormDTO = new ServiceQueryFormDTO.Builder(serviceDef)
        .interfaces(CommonConstants.HTTP_SECURE_JSON).build();

    final String authInfo = onboardingHandler.getAuthInfo();
    final SystemRequestDTO systemRequest = getSystemRequest(ipAddress, port, authInfo);

    final OrchestrationResponseDTO orchestrationResponseDTO = onboardingHandler
        .lookupOrchestration(serviceQueryFormDTO, systemRequest);

    return onboardingHandler.createUri(orchestrationResponseDTO.getResponse().get(0));
  }

  private DeviceRequestDTO getDeviceRequest(String ipAddress, String macAddress, String authInfo) {
    return new DeviceRequestDTO(commonName, ipAddress, macAddress, authInfo);
  }

  private SystemRequestDTO getSystemRequest(final String ipAddress, final int port, final String authInfo) {
    return new SystemRequestDTO(commonName, ipAddress, port, authInfo);
  }

}
