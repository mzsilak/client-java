package eu.arrowhead.client.station;

import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO.Builder;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.dto.RfidResponseDTO;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.onboarding.HttpClient;
import eu.arrowhead.demo.utils.ProcessTemplate;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
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
    private final ProcessTemplate rfidTemplate;
    private final ChargingStationApplication application;

    private final Set<String> cache = new ConcurrentSkipListSet<>();
    private final Semaphore chargingLock = new Semaphore(1);
    private Process rfidProcess = null;


    public ChargingStationService(final ExecutorService executorService,
                                  @Qualifier("redControl") final ControllableLed red,
                                  @Qualifier("rfid") final ProcessTemplate rfidTemplate,
                                  final PowerHandler powerHandler, final ArrowheadHandler arrowhead,
                                  final HttpClient httpClient, final ChargingStationApplication application) {
        this.executorService = executorService;
        this.red = red;
        this.powerHandler = powerHandler;
        this.arrowhead = arrowhead;
        this.httpClient = httpClient;
        this.rfidTemplate = rfidTemplate;
        this.application = application;

        // watch all the time
        logger.info("New instance of {}", getClass().getSimpleName());
        rfidTemplate.setInputStreamConsumer(this::processRfid);
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

    public synchronized boolean charge(final String rfid) {

        if (Objects.isNull(rfid)) {
            return false;
        }

        if (chargingLock.availablePermits() == 0) {
            logger.debug("Station is already charging");
            return false;
        }

        try {
            chargingLock.acquire();
            logger.info("New charging request for {}", rfid);
            red.blink();

            logger.info("Finding and contacting car with RFID {}", rfid);
            final ServiceQueryFormDTO queryForm = new Builder(Constants.SERVICE_CAR_RFID).metadata("rfid", rfid)
                                                                                         .build();

            final SystemRequestDTO requester = application.getSystemRequest();
            final UriComponents carUri = arrowhead.createUri(queryForm, requester);
            final ResponseEntity<RfidResponseDTO> rfidEntity = httpClient
                .sendRequest(carUri, HttpMethod.GET, RfidResponseDTO.class);
            final RfidResponseDTO entityBody = rfidEntity.getBody();

            if (Objects.isNull(entityBody) || Objects.isNull(entityBody.getRfid())) {
                Thread.sleep(1000L);
                return false;
            }

            if (!(rfid.equals(entityBody.getRfid()) && cache.contains(rfid))) {
                logger.info("Unknown RFID: {}", rfid);
                return false;
            }

            logger.info("Ready to charge RFID: {}", rfid);
            executorService.execute(new Charger(rfid, chargingLock));
            return true;
        } catch (final Exception e) {
            logger.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
            chargingLock.release();
            return false;
        } finally {
            red.turnOff();
        }
    }

    @EventListener(OnboardingFinishedEvent.class)
    public void initProcesses() throws IOException {
        logger.info("Starting RFID process");
        rfidProcess = rfidTemplate.execute();
    }

    @PreDestroy
    public void stopProcesses() {
        logger.info("Stopping RFID process");
        if (Objects.nonNull(rfidProcess)) {
            rfidProcess.destroy();
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
        private final Semaphore chargingLock;

        public Charger(final String rfid, final Semaphore chargingLock) {
            this.rfid = rfid;
            this.chargingLock = chargingLock;
        }

        @Override
        public void run() {
            try {
                logger.info("Starting charge for {}", rfid);
                red.turnOn();
                powerHandler.turnOn();
                powerHandler.statistics();
                powerHandler.turnOff();
                logger.info("Charging done for {}", rfid);
            } finally {
                red.turnOff();
                chargingLock.release();
            }
        }
    }
}
