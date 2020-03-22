package eu.arrowhead.demo.grovepi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.devices.GroveLed;

public class ControllableLed {

    private final Logger logger = LogManager.getLogger();
    private final BlinkingLedRunnable blinking;
    private final GroveLed led;

    public ControllableLed(final ExecutorService executorService, final GroveLed led) {
        blinking = new BlinkingLedRunnable(executorService, led);
        this.led = led;
    }

    public void turnOff() {
        try {
            blinking.stop();
            led.set(false);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void blink() {
        blinking.start();
    }

    public void turnOn() {
        try {
            blinking.stop();
            led.set(true);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
