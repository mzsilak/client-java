package eu.arrowhead.demo.grovepi;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.devices.GroveLed;

public class BlinkingLedRunnable implements Runnable {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Future<?>> reference = new AtomicReference<>(null);
    private final ExecutorService executorService;
    private final GroveLed led;
    private boolean state = false;

    public BlinkingLedRunnable(final ExecutorService executorService, final GroveLed led) {
        this.executorService = executorService;
        this.led = led;
    }

    public synchronized void start() {
        if (!running.compareAndExchange(false, true)) {

            final Future<?> oldRef = reference.getAndSet(executorService.submit(this));
            if (Objects.nonNull(oldRef)) {
                oldRef.cancel(true);
            }
        } else {
            logger.debug("Ignored start on " + led.toString());
        }
    }

    public synchronized void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                logger.trace("blink " + led.toString());
                state = !state;
                led.set(state);
                Thread.sleep(500L);
            } catch (IOException | InterruptedException e) {
                running.set(false);
                logger.error(e.getMessage());
            }
        }
        logger.debug("Finished blinking on " + led.toString());
    }
}