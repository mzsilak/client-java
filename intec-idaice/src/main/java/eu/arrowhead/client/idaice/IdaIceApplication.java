package eu.arrowhead.client.idaice;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateCreationRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.utils.IpUtilities;
import eu.arrowhead.demo.web.HttpServer;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;

@Service
public class IdaIceApplication {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean onboarded = new AtomicBoolean(false);
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HttpServer httpServer;

    private final ArrowheadHandler onboardingHandler;
    private final String commonName;

    private final String ipAddress;
    private final String macAddress;
    private final String validity;
    private final int port;

    private ApplicationReadyEvent applicationReadyEvent = null;
    private SystemResponseDTO systemResponseDTO = null;

    @Autowired
    public IdaIceApplication(final ApplicationEventPublisher applicationEventPublisher, final HttpServer httpServer,
                             final ArrowheadHandler onboardingHandler, @Value("${server.name}") final String commonName)
        throws IOException {
        this.applicationEventPublisher = applicationEventPublisher;
        this.httpServer = httpServer;
        this.onboardingHandler = onboardingHandler;
        this.commonName = commonName;

        httpServer.configureName(commonName);

        ipAddress = IpUtilities.getAddressString();
        macAddress = IpUtilities.getMacAddress(ipAddress);
        validity = Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now().plusDays(1));
        port = httpServer.getPort();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void performOnboarding(final ApplicationReadyEvent event) {
        try {
            logger.info("Start onboarding ...");

            final var creationRequest = new CertificateCreationRequestDTO(commonName);
            onboardingHandler.onboard(new OnboardingWithNameRequestDTO(creationRequest));

            final String authInfo = onboardingHandler.getAuthInfo();

            final var deviceRequest = getDeviceRegistryRequest(ipAddress, macAddress, validity, authInfo,
                                                               creationRequest);
            final var systemRequest = getSystemRegistryRequest(ipAddress, macAddress, port, validity, authInfo,
                                                               creationRequest);
            onboardingHandler.registerDevice(deviceRequest);
            systemResponseDTO = onboardingHandler.registerSystem(systemRequest);

            applicationEventPublisher.publishEvent(new OnboardingFinishedEvent(this));
            onboarded.set(true);
            applicationReadyEvent = event;

        } catch (final Exception e) {
            logger.warn("Onboarding issue: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            applicationEventPublisher.publishEvent(
                new ApplicationFailedEvent(event.getSpringApplication(), event.getArgs(), event.getApplicationContext(),
                                           e));
            performOffboarding();
        }
    }

    private DeviceRegistryOnboardingWithNameRequestDTO getDeviceRegistryRequest(String ipAddress, String macAddress,
                                                                                String validity, String authInfo,
                                                                                CertificateCreationRequestDTO creationRequest) {

        return new DeviceRegistryOnboardingWithNameRequestDTO(getDeviceRequest(ipAddress, macAddress, authInfo),
                                                              validity, null, null, creationRequest);
    }

    private SystemRegistryOnboardingWithNameRequestDTO getSystemRegistryRequest(String ipAddress, String macAddress,
                                                                                final int port, String validity,
                                                                                String authInfo,
                                                                                CertificateCreationRequestDTO creationRequest) {
        return new SystemRegistryOnboardingWithNameRequestDTO(getSystemRequest(ipAddress, port, authInfo),
                                                              getDeviceRequest(ipAddress, macAddress, authInfo),
                                                              validity, null, null, creationRequest);
    }

    @PreDestroy
    public void performOffboarding() {
        try {
            httpServer.stop();

            logger.info("Unregistering myself ...");

            onboardingHandler.unregisterSystem(commonName, ipAddress, port);
            onboardingHandler.unregisterDevice(commonName, macAddress);
            systemResponseDTO = null;

        } catch (final Exception e) {
            logger.warn("Offboarding issue: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        } finally {
            onboarded.set(false);

            if (applicationReadyEvent != null) {
                applicationEventPublisher.publishEvent(
                    new ApplicationFailedEvent(applicationReadyEvent.getSpringApplication(),
                                               applicationReadyEvent.getArgs(),
                                               applicationReadyEvent.getApplicationContext(),
                                               new Exception("Unknown")));
            }
        }
    }

    private DeviceRequestDTO getDeviceRequest(String ipAddress, String macAddress, String authInfo) {
        return new DeviceRequestDTO(commonName, ipAddress, macAddress, authInfo);
    }

    private SystemRequestDTO getSystemRequest(final String ipAddress, final int port, final String authInfo) {
        return new SystemRequestDTO(commonName, ipAddress, port, authInfo);
    }

    @EventListener(OnboardingFinishedEvent.class)
    public void startWebServer() throws IOException {
        httpServer.init();
        httpServer.start();
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

    public SystemResponseDTO getSystemResponseDTO() {
        return systemResponseDTO;
    }
}
