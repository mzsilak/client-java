package eu.arrowhead.client.station;

import eu.arrowhead.demo.utils.LoggingStreamGobbler;
import eu.arrowhead.demo.utils.ProcessTemplate;
import java.io.IOException;
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

    @Autowired
    public PowerHandler(@Qualifier("powerOn") ProcessTemplate powerOn, @Qualifier("powerOff") ProcessTemplate powerOff,
                        @Qualifier("powerRead") ProcessTemplate powerRead) {
        super();
        this.powerOn = powerOn;
        this.powerOff = powerOff;
        this.powerRead = powerRead;
    }

    public void turnOn() {
        try {
            final Process process = powerOn.executeWithGobblers();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }

    public void turnOff() {
        try {
            final Process process = powerOff.executeWithGobblers();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }

    public void read() {
        try {
            final Process process = powerRead.execute();
            powerRead.startGobbler(new LoggingStreamGobbler(process.getInputStream(), Level.INFO, "POWER: "));
            powerRead.startGobbler(new LoggingStreamGobbler(process.getErrorStream(), Level.ERROR, "POWER: "));
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }
}
