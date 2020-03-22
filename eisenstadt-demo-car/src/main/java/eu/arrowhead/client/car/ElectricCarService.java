package eu.arrowhead.client.car;

import eu.arrowhead.demo.grovepi.ControllableLed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ElectricCarService {

    private final Logger logger = LogManager.getLogger();
    private final ControllableLed red;
    private final String rfid;

    public ElectricCarService(@Qualifier("redControl") final ControllableLed red,
                              @Value("${server.rfid}") final String rfid) {
        this.red = red;
        this.rfid = rfid;
    }

    public String getRfid() {
        red.blink();
        logger.info("RFID requested");
        sleep();
        red.turnOff();
        return rfid;
    }

    private void sleep() {
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            logger.warn("Interrupted!", e);
        }
    }
}
