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

  private AtomicReference<Future<?>> futureReference = new AtomicReference<>();
  private ButtonPressedListener listener = null;


  public GroveButtonObserver(final ExecutorService executorService, final GrovePi grovePi, final int digitalPort)
      throws IOException {
    this.executorService = executorService;
    button = grovePi.getDigitalIn(digitalPort); // D6
    button.setListener(this);
  }

  public synchronized void start() {
    if (Objects.nonNull(futureReference.get())) {
      running.set(true);
      final Future<?> submit = executorService.submit(this);
      futureReference.set(submit);
    }
  }

  public synchronized void stop() {
    running.set(false);
    futureReference.getAndSet(null).cancel(true);
  }

  @Override
  public void run() {
    while (running.get()) {
      try {
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
    if (listener == null) {
      return;
    }
    if (!oldValue && newValue) {
      listener.trigger();
    }
  }
}
