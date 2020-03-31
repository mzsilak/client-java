package eu.arrowhead.client.station;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateCreationRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.events.OffboardingFinishedEvent;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.utils.IpUtilities;
import eu.arrowhead.demo.web.HttpServer;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ServiceConfigurationError;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ChargingStationApplication {

    private final Logger logger = LogManager.getLogger();
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HttpServer httpServer;

    private final ArrowheadHandler onboardingHandler;
    private final String commonName;

    private final ControllableLed green;
    private final ControllableLed red;

    private final String ipAddress;
    private final String macAddress;
    private final String validity;
    private final int port;

    @Autowired
    public ChargingStationApplication(final ApplicationEventPublisher applicationEventPublisher,
                                      final HttpServer httpServer, final ArrowheadHandler onboardingHandler,
                                      @Value("${server.name}") final String commonName,
                                      @Qualifier("greenControl") final ControllableLed green,
                                      @Qualifier("redControl") final ControllableLed red) throws IOException {
        this.applicationEventPublisher = applicationEventPublisher;
        this.httpServer = httpServer;
        this.onboardingHandler = onboardingHandler;
        this.commonName = commonName;
        this.green = green;
        this.red = red;

        httpServer.configureName(commonName);

        ipAddress = IpUtilities.getAddressString();
        macAddress = IpUtilities.getMacAddress(ipAddress);
        validity = Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now().plusDays(1));
        port = httpServer.getPort();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void performOnboarding() {

        try {
            green.blink();
            red.blink();

            final var creationRequest = new CertificateCreationRequestDTO(commonName);
            onboardingHandler.onboard(new OnboardingWithNameRequestDTO(creationRequest));
            final String authInfo = onboardingHandler.getAuthInfo();

            onboardingHandler
                .registerDevice(getDeviceRegistryRequest(ipAddress, macAddress, validity, authInfo, creationRequest));
            onboardingHandler.registerSystem(
                getSystemRegistryRequest(ipAddress, macAddress, port, validity, authInfo, creationRequest));
            onboardingHandler.registerService(
                getServiceRegistryRequest(ipAddress, port, validity, authInfo, Constants.OP_STATION_CHARGE_URI,
                                          Constants.SERVICE_STATION_CHARGE));
            onboardingHandler.registerService(
                getServiceRegistryRequest(ipAddress, port, validity, authInfo, Constants.OP_STATION_REGISTER_URI,
                                          Constants.SERVICE_STATION_REGISTER));
            onboardingHandler.registerService(
                getServiceRegistryRequest(ipAddress, port, validity, authInfo, Constants.OP_STATION_UNREGISTER_URI,
                                          Constants.SERVICE_STATION_UNREGISTER));

            logger.info("Firing OnboardingFinishedEvent ...");
            applicationEventPublisher.publishEvent(new OnboardingFinishedEvent(this));
        } catch (final Exception e) {
            logger.warn("Issues during onboarding: {}", e.getMessage());
            performOffboarding();
            throw new ServiceConfigurationError(e.getMessage(), e);
        }

        red.turnOff();
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

    private ServiceRegistryRequestDTO getServiceRegistryRequest(String ipAddress, final int port, final String validity,
                                                                final String authInfo, final String uriPostfix,
                                                                final String serviceDef) {
        final ServiceRegistryRequestDTO requestDTO = new ServiceRegistryRequestDTO();
        requestDTO.setSecure(ServiceSecurityType.CERTIFICATE.name());
        requestDTO.setServiceUri(Constants.STATION_CONTROLLER_PATH + uriPostfix);
        requestDTO.setInterfaces(List.of(CommonConstants.HTTP_SECURE_JSON));
        requestDTO.setEndOfValidity(validity);
        requestDTO.setProviderSystem(getSystemRequest(ipAddress, port, authInfo));
        requestDTO.setServiceDefinition(serviceDef);
        return requestDTO;
    }

    @PreDestroy
    public void performOffboarding() {
        try {
            logger.info("Unregistering myself ...");
            httpServer.stop();
            green.blink();
            red.blink();

            final String ipAddress = IpUtilities.getAddressString();
            final String macAddress = IpUtilities.getMacAddress();
            final int port = httpServer.getPort();

            onboardingHandler.unregisterService(Constants.SERVICE_STATION_CHARGE, commonName, ipAddress, port);
            onboardingHandler.unregisterService(Constants.SERVICE_STATION_REGISTER, commonName, ipAddress, port);
            onboardingHandler.unregisterService(Constants.SERVICE_STATION_UNREGISTER, commonName, ipAddress, port);
            onboardingHandler.unregisterSystem(commonName, ipAddress, port);
            onboardingHandler.unregisterDevice(commonName, macAddress);

            stopLeds();
        } catch (final Exception e) {
            logger.warn("Issues during offboarding: {}", e.getMessage());
        }
    }

    private DeviceRequestDTO getDeviceRequest(String ipAddress, String macAddress, String authInfo) {
        return new DeviceRequestDTO(commonName, ipAddress, macAddress, authInfo);
    }

    private SystemRequestDTO getSystemRequest(final String ipAddress, final int port, final String authInfo) {
        return new SystemRequestDTO(commonName, ipAddress, port, authInfo);
    }

    @EventListener(OffboardingFinishedEvent.class)
    public void stopLeds() {
        green.turnOff();
        red.turnOff();
    }

    public SystemRequestDTO getSystemRequest() {
        return new SystemRequestDTO(commonName, ipAddress, port, onboardingHandler.getAuthInfo());
    }

    @EventListener(OnboardingFinishedEvent.class)
    public void startWebServer() throws IOException {
        httpServer.init();
        httpServer.start();
        green.turnOn();
    }

}
