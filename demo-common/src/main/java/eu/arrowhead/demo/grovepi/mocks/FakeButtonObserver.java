package eu.arrowhead.demo.grovepi.mocks;

import eu.arrowhead.demo.grovepi.ButtonPressedListener;
import eu.arrowhead.demo.grovepi.GroveButtonObserver;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GrovePi;

public class FakeButtonObserver extends GroveButtonObserver {

  private final Logger logger = LogManager.getLogger();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ExecutorService executorService;
  private final int digitalPort;

  private AtomicReference<Future<?>> futureReference = new AtomicReference<>();
  private ButtonPressedListener listener = null;


  public FakeButtonObserver(final ExecutorService executorService, final GrovePi grovePi, final int digitalPort)
      throws IOException {
    super(executorService, grovePi, digitalPort);
    this.executorService = executorService;
    this.digitalPort = digitalPort;
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
        Thread.sleep(500L);
      } catch (InterruptedException e) {
        logger.debug(e.getMessage());
      }
    }
  }

  public void setListener(final ButtonPressedListener listener) {
    this.listener = listener;
  }

  public void fakeTrigger() {
    if (running.get() && Objects.nonNull(listener)) {
      logger.debug("Fake button press event received");
      listener.trigger();
    }
  }
}
