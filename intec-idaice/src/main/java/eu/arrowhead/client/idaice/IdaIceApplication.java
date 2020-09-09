package eu.arrowhead.client.idaice;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateCreationRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class IdaIceApplication {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean onboarded = new AtomicBoolean(false);

    private final ArrowheadHandler onboardingHandler;
    private final CertificateCreationRequestDTO certificateCreationRequestDTO;
    private final DeviceRequestDTO deviceRequestDTO;
    private final SystemRequestDTO systemRequestDTO;

    private final IdaIceProcessStarter processStarter;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public IdaIceApplication(final ApplicationEventPublisher applicationEventPublisher,
                             final ArrowheadHandler onboardingHandler,
                             final CertificateCreationRequestDTO certificateCreationRequestDTO,
                             final DeviceRequestDTO deviceRequestDTO, final SystemRequestDTO systemRequestDTO,
                             final IdaIceProcessStarter processStarter) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.onboardingHandler = onboardingHandler;
        this.certificateCreationRequestDTO = certificateCreationRequestDTO;
        this.deviceRequestDTO = deviceRequestDTO;
        this.systemRequestDTO = systemRequestDTO;
        this.processStarter = processStarter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void performOnboarding(final ApplicationReadyEvent event) {
        try {
            logger.info("Start onboarding ...");

            onboardingHandler.onboard(new OnboardingWithNameRequestDTO(certificateCreationRequestDTO));

            final String authInfo = onboardingHandler.getAuthInfo();
            deviceRequestDTO.setAuthenticationInfo(authInfo);
            systemRequestDTO.setAuthenticationInfo(authInfo);

            onboardingHandler.registerDevice(getDeviceRegistryRequest());
            onboardingHandler.registerSystem(getSystemRegistryRequest());

            applicationEventPublisher.publishEvent(new OnboardingFinishedEvent(this));
            onboarded.set(true);

            processStarter.startBuildingTracker();
        } catch (IOException e) {
            logger.warn("Issue during application start: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            applicationEventPublisher.publishEvent(createFrom(event, e));
        } catch (final Exception e) {
            logger.warn("Onboarding issue: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            applicationEventPublisher.publishEvent(createFrom(event, e));
        }
    }

    private ApplicationFailedEvent createFrom(final ApplicationReadyEvent event, final Exception e) {
        return new ApplicationFailedEvent(event.getSpringApplication(), event.getArgs(), event.getApplicationContext(),
                                          e);
    }

    public DeviceRegistryOnboardingWithNameRequestDTO getDeviceRegistryRequest() {

        return new DeviceRegistryOnboardingWithNameRequestDTO(deviceRequestDTO, validity(), null, null,
                                                              certificateCreationRequestDTO);
    }

    public SystemRegistryOnboardingWithNameRequestDTO getSystemRegistryRequest() {
        return new SystemRegistryOnboardingWithNameRequestDTO(systemRequestDTO, deviceRequestDTO, validity(), null,
                                                              null, certificateCreationRequestDTO);
    }

    @PreDestroy
    public void performOffboarding() {
        if (!onboarded.get()) {
            return;
        }

        try {
            logger.info("Unregistering myself ...");

            onboardingHandler.unregisterSystem(systemRequestDTO.getSystemName(), systemRequestDTO.getAddress(),
                                               systemRequestDTO.getPort());
            onboardingHandler.unregisterDevice(deviceRequestDTO.getDeviceName(), deviceRequestDTO.getMacAddress());

        } catch (final Exception e) {
            logger.warn("Offboarding issue: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        } finally {
            onboarded.set(false);
        }
    }

    private String validity() {
        return Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now().plusDays(1));
    }
}
