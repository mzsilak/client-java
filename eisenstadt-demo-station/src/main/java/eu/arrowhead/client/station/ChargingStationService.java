package eu.arrowhead.client.station;

import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO.Builder;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.dto.RfidResponseDTO;
import eu.arrowhead.demo.events.OnboardingFinishedEvent;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import eu.arrowhead.demo.onboarding.HttpClient;
import eu.arrowhead.demo.utils.ProcessTemplate;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
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

    private final Logger logger = LogManager.getLogger();
    private final ExecutorService executorService;
    private final ControllableLed red;
    private final ProcessTemplate rfidTemplate;
    private final PowerHandler powerHandler;
    private final ArrowheadHandler arrowhead;
    private final HttpClient httpClient;

    private final Set<String> cache = new ConcurrentSkipListSet<String>();
    private final AtomicBoolean charging = new AtomicBoolean(false);

    private Process rfidProcess = null;

    public ChargingStationService(final ExecutorService executorService,
                                  @Qualifier("redControl") final ControllableLed red,
                                  @Qualifier("rfid") final ProcessTemplate rfidTemplate,
                                  final PowerHandler powerHandler, ArrowheadHandler arrowhead, HttpClient httpClient) {
        this.executorService = executorService;
        this.red = red;
        this.rfidTemplate = rfidTemplate;
        this.powerHandler = powerHandler;
        this.arrowhead = arrowhead;
        this.httpClient = httpClient;

        // watch all the time
        rfidTemplate.setInputStreamConsumer(this::processRfid);
    }

    public void processRfid(final String string) {
        try {
            logger.debug("New output from RFID reader: {}", string);
            Scanner scanner = new Scanner(string);
            scanner.findInLine("Card read UID: ");
            if (scanner.hasNext()) {
                charge(scanner.next());
            }
        } catch (final IOException | InterruptedException e) {
            logger.warn("Unable to execute charge request: {}", e.getMessage());
        }
    }

    public boolean charge(final String rfid) throws IOException, InterruptedException {

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
            final ServiceQueryFormDTO queryForm = new Builder(Constants.SERVICE_CAR_RFID).metadata("rfid", rfid)
                                                                                         .build();
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
        } finally {
            red.turnOff();
        }

        logger.info("Ready to charge RFID: {}", rfid);
        powerHandler.turnOn();
        executorService.execute(new Charger(rfid));
        return true;
    }

    @EventListener(OnboardingFinishedEvent.class)
    public void initProcesses() throws IOException {
        rfidProcess = rfidTemplate.executeWithGobblers();
    }

    @PreDestroy
    public void stopProcesses() throws IOException {
        if (Objects.nonNull(rfidProcess)) {
            rfidProcess.destroyForcibly();
        }
    }

    public boolean register(final String rfid) {
        // TODO contact arrowhead?
        return cache.add(rfid);
    }

    public boolean unregister(final String rfid) {
        // TODO contact arrowhead?
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
