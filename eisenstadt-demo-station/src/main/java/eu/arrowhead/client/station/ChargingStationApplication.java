package eu.arrowhead.client.station;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.DeviceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.events.OffboardingFinishedEvent;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.onboarding.HttpClient;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.ssl.SSLException;
import eu.arrowhead.demo.utils.IpUtilities;
import eu.arrowhead.demo.web.HttpServer;
import java.io.IOException;
import java.net.SocketException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.List;
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
    private final HttpService httpClient;
    private final String commonName;

    private final ControllableLed green;
    private final ControllableLed red;

    @Autowired
    public ChargingStationApplication(final ApplicationEventPublisher applicationEventPublisher,
                                      final HttpServer httpServer, final ArrowheadHandler onboardingHandler,
                                      final HttpClient httpClient, @Value("${server.name}") final String commonName,
                                      @Qualifier("greenControl") final ControllableLed green,
                                      @Qualifier("redControl") final ControllableLed red)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SSLException,
               InvalidKeySpecException {
        this.applicationEventPublisher = applicationEventPublisher;
        this.httpServer = httpServer;
        this.onboardingHandler = onboardingHandler;
        this.httpClient = httpClient;
        this.commonName = commonName;
        this.green = green;
        this.red = red;

        httpServer.configureName(commonName);
        onboardingHandler.performOnboarding(new OnboardingWithNameRequestDTO(commonName));
        performOffboarding();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void performOnboarding() {

        try {
            green.blink();
            red.blink();

            final String ipAddress = IpUtilities.getIpAddress();
            final String macAddress = IpUtilities.getMacAddress();
            final String validity = Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now().plusDays(1));
            final int port = httpServer.getPort();

            final String authInfo = onboardingHandler.getAuthInfo();

            onboardingHandler.registerDevice(getDeviceRegistryRequest(ipAddress, macAddress, validity, authInfo));
            onboardingHandler.registerSystem(getSystemRegistryRequest(ipAddress, macAddress, port, validity, authInfo));
            onboardingHandler.registerService(
                getServiceRegistryRequest(ipAddress, port, validity, authInfo, Constants.OP_STATION_CHARGE_URI,
                                          Constants.SERVICE_STATION_CHARGE));
            onboardingHandler.registerService(
                getServiceRegistryRequest(ipAddress, port, validity, authInfo, Constants.OP_STATION_REGISTER_URI,
                                          Constants.SERVICE_STATION_REGISTER));
            onboardingHandler.registerService(
                getServiceRegistryRequest(ipAddress, port, validity, authInfo, Constants.OP_STATION_UNREGISTER_URI,
                                          Constants.SERVICE_STATION_UNREGISTER));
            applicationEventPublisher.publishEvent(new OnboardingFinishedEvent(this));
            red.turnOff();
        } catch (final Exception e) {
            logger.warn("Issues during onboarding: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void performOffboarding() {
        try {
            httpServer.stop();
            green.blink();
            red.blink();

            final String ipAddress = IpUtilities.getIpAddress();
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

    private DeviceRegistryRequestDTO getDeviceRegistryRequest(String ipAddress, String macAddress, String validity,
                                                              String authInfo) {

        return new DeviceRegistryRequestDTO(getDeviceRequest(ipAddress, macAddress, authInfo), validity, null, null);
    }

    private SystemRegistryRequestDTO getSystemRegistryRequest(String ipAddress, String macAddress, final int port,
                                                              String validity, String authInfo) {
        return new SystemRegistryRequestDTO(getSystemRequest(ipAddress, port, authInfo),
                                            getDeviceRequest(ipAddress, macAddress, authInfo), validity, null, null);
    }

    private ServiceRegistryRequestDTO getServiceRegistryRequest(String ipAddress, final int port, final String validity,
                                                                final String authInfo, final String uriPostfix,
                                                                final String serviceDef) throws SocketException {
        final ServiceRegistryRequestDTO requestDTO = new ServiceRegistryRequestDTO();
        requestDTO.setSecure(ServiceSecurityType.CERTIFICATE.name());
        requestDTO.setServiceUri(Constants.STATION_CONTROLLER_PATH + uriPostfix);
        requestDTO.setInterfaces(List.of(CommonConstants.HTTP_SECURE_JSON));
        requestDTO.setEndOfValidity(validity);
        requestDTO.setProviderSystem(getSystemRequest(ipAddress, port, authInfo));
        requestDTO.setServiceDefinition(serviceDef);
        return requestDTO;
    }

    @EventListener(OffboardingFinishedEvent.class)
    public void stopLeds() {
        green.turnOff();
        red.turnOff();
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
        green.turnOn();
    }
}
