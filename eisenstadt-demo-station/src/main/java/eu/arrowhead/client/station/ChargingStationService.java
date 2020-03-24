package eu.arrowhead.client.station;

import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO.Builder;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.dto.RfidResponseDTO;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.onboarding.HttpClient;
import eu.arrowhead.demo.utils.ProcessInputHandler;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;

@Service
public class ChargingStationService {

    private final static String CARD_UID = "Card read UID: ";
    private final Logger logger = LogManager.getLogger();
    private final ExecutorService executorService;
    private final ControllableLed red;
    private final PowerHandler powerHandler;
    private final ArrowheadHandler arrowhead;
    private final HttpClient httpClient;

    private final Set<String> cache = new ConcurrentSkipListSet<String>();
    private final AtomicBoolean charging = new AtomicBoolean(false);

    private ProcessInputHandler rfidHandler = null;

    public ChargingStationService(final ExecutorService executorService,
                                  @Qualifier("redControl") final ControllableLed red, final PowerHandler powerHandler,
                                  final ArrowheadHandler arrowhead, final HttpClient httpClient) {
        this.executorService = executorService;
        this.red = red;
        this.powerHandler = powerHandler;
        this.arrowhead = arrowhead;
        this.httpClient = httpClient;

        // watch all the time
        logger.info("New instance of {}", getClass().getSimpleName());
    }

    public void processRfid(final String string) {
        try {
            if (StringUtils.startsWith(string, CARD_UID)) {
                charge(StringUtils.remove(string, CARD_UID));
            }
        } catch (final Exception e) {
            logger.warn("Unable to execute charge request: {}", e.getMessage());
        }
    }

    public boolean charge(final String rfid) {

        if (Objects.isNull(rfid)) {
            return false;
        }

        if (charging.compareAndExchange(false, true)) {
            logger.debug("Station is already charging");
            return false;
        }

        try {
            logger.info("New charging request for {}", rfid);
            red.blink();

            logger.info("Finding and contacting car with RFID {}", rfid);
            final ServiceQueryFormDTO queryForm = new Builder(Constants.SERVICE_CAR_RFID).metadata("rfid", rfid).build();
            final UriComponents carUri = arrowhead.createUri(queryForm);
            final ResponseEntity<RfidResponseDTO> rfidEntity = httpClient
                .sendRequest(carUri, HttpMethod.GET, RfidResponseDTO.class);
            final RfidResponseDTO entityBody = rfidEntity.getBody();

            if (Objects.isNull(entityBody) || Objects.isNull(entityBody.getRfid())) {
                return false;
            }

            if (!(rfid.equals(entityBody.getRfid()) && cache.contains(rfid))) {
                logger.info("Unknown RFID: {}", rfid);
                return false;
            }
        } catch (final Exception e) {
            charging.set(false);
            throw e;
        } finally {
            red.turnOff();
        }

        logger.info("Ready to charge RFID: {}", rfid);
        powerHandler.turnOn();
        executorService.execute(new Charger(rfid));
        return true;
    }

    @EventListener(OnboardingFinishedEvent.class)
    public void initProcesses() {
        logger.info("Starting RFID process");
        if (Objects.nonNull(rfidHandler)) {
            rfidHandler.stop();
        }
    }

    @PreDestroy
    public void stopProcesses() {
        logger.info("Stopping RFID process");
        if (Objects.nonNull(rfidHandler)) {
            rfidHandler.stop();
        }
    }

    public boolean register(final String rfid) {
        logger.info("Register RFID in cache: {}", rfid);
        return cache.add(rfid);
    }

    public boolean unregister(final String rfid) {
        logger.info("Removing RFID from cache: {}", rfid);
        return cache.remove(rfid);
    }

    private class Charger implements Runnable {

        private final String rfid;

        public Charger(final String rfid) {
            this.rfid = rfid;
        }

        @Override
        public void run() {
            try {
                logger.info("Starting charge for {}", rfid);
                charging.set(true);
                red.turnOn();
                powerHandler.turnOn();
                powerHandler.read();
                powerHandler.turnOff();
                logger.info("Charging done for {}", rfid);
            } finally {
                red.turnOff();
                charging.set(false);
            }
        }
    }
}
