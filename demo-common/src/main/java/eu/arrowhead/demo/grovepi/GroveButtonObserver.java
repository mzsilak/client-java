package eu.arrowhead.demo.grovepi;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GroveDigitalInListener;
import org.iot.raspberry.grovepi.GrovePi;

public class GroveButtonObserver implements Runnable, GroveDigitalInListener {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ExecutorService executorService;
    private final GroveDigitalIn button;

    private AtomicReference<Future<?>> futureReference = new AtomicReference<>(null);
    private ButtonPressedListener listener = null;


    public GroveButtonObserver(final ExecutorService executorService, final GrovePi grovePi, final int digitalPort)
        throws IOException {
        this.executorService = executorService;
        button = grovePi.getDigitalIn(digitalPort);
        button.setListener(this);
    }

    public synchronized void start() {
        if (Objects.isNull(futureReference.get())) {
            logger.info("start");
            running.set(true);
            final Future<?> submit = executorService.submit(this);
            futureReference.set(submit);
        } else {
            logger.warn("Ignore call to start as it is running already");
        }
    }

    public synchronized void stop() {
        logger.info("stop");
        running.set(false);
        final Future<?> oldReference = futureReference.getAndSet(null);
        if (Objects.nonNull(oldReference)) {
            oldReference.cancel(true);
        } else {
            logger.warn("Ignore call to stop as it is not running");
        }
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                logger.trace("fetch button");
                button.get();
                Thread.sleep(200L);
            } catch (InterruptedException | IOException e) {
                logger.debug(e.getMessage());
            }
        }
    }

    public void setListener(final ButtonPressedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onChange(boolean oldValue, boolean newValue) {
        logger.info("Change detected - oldValue:{}, newValue:{}", oldValue, newValue);
        if (listener == null) {
            return;
        }
        if (!oldValue && newValue) {
            listener.trigger();
        }
    }
}
