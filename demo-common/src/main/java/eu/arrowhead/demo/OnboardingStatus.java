package eu.arrowhead.demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnboardingStatus
{
    private final Logger logger = LogManager.getLogger();
    private final ExecutorService executorService;
    private final GrovePi grovePi;
    private final GroveLed red;
    private final GroveLed green;

    private final BlinkingLed blinkingRed;
    private final BlinkingLed blinkingGreen;

    public OnboardingStatus(final ExecutorService executorService, final GrovePi grovePi) throws IOException {
        this.grovePi = grovePi;
        this.executorService = executorService;
        red = new GroveLed(grovePi, 3); // D3
        green = new GroveLed(grovePi, 4); // D4

        blinkingRed = new BlinkingLed(red);
        blinkingGreen = new BlinkingLed(green);
    }

    public void disconnected()
    {
        try {
            blinkingRed.stop();
            blinkingGreen.stop();
            red.set(false);
            green.set(false);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            error();
        }
    }

    public void connecting()
    {
        try {
            blinkingRed.stop();
            blinkingGreen.reset();
            red.set(false);
            executorService.execute(blinkingGreen);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            error();
        }
    }

    public void connected()
    {
        try {
            blinkingRed.stop();
            blinkingGreen.stop();
            red.set(false);
            green.set(true);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            error();
        }
    }

    public void error()
    {
        try {
            blinkingRed.reset();
            blinkingGreen.stop();
            green.set(false);
            executorService.execute(blinkingRed);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        red.set(false);
        green.set(false);
        executorService.shutdown();
        blinkingRed.stop();
        blinkingGreen.stop();
    }

    private class BlinkingLed implements Runnable
    {
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final GroveLed led;
        private boolean state = false;

        public BlinkingLed(final GroveLed led) {
            this.led = led;
        }

        public void reset()
        {
            running.set(true);
        }

        public void stop()
        {
            running.set(false);
        }

        @Override
        public void run() {
            while(running.get())
            {
                try {
                    state = !state;
                    led.set(state);
                    Thread.sleep(500L);
                } catch (IOException | InterruptedException e) {
                    running.set(false);
                    logger.error(e.getMessage());
                }
            }
        }
    }
}
