package eu.arrowhead.client.station;

import eu.arrowhead.demo.utils.LoggingStreamGobbler;
import eu.arrowhead.demo.utils.ProcessTemplate;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PowerHandler {

    private final Logger logger = LogManager.getLogger();
    private final ProcessTemplate powerOn;
    private final ProcessTemplate powerOff;
    private final ProcessTemplate powerRead;
    private final AtomicBoolean charging = new AtomicBoolean(false);

    @Autowired
    public PowerHandler(@Qualifier("powerOn") ProcessTemplate powerOn, @Qualifier("powerOff") ProcessTemplate powerOff,
                        @Qualifier("powerRead") ProcessTemplate powerRead) {
        super();
        this.powerOn = powerOn;
        this.powerOff = powerOff;
        this.powerRead = powerRead;

        powerRead.setInputStreamConsumer(this::reportPowerUsage);
    }

    public void turnOn() {
        try {
            charging.set(true);
            logger.info("Turning power on");
            final Process process = powerOn.execute();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }

    public void turnOff() {
        try {
            logger.info("Turning power off");
            final Process process = powerOff.execute();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        } finally {
            charging.set(false);
        }
    }

    public void statistics() {
        try {
            logger.info("Running statistics");
            final Process process = powerRead.execute();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }

    public void reportPowerUsage(final String line) {
        logger.info("Statistics: {}", line);
    }

    public boolean isCharging() {
        return charging.get();
    }
}
