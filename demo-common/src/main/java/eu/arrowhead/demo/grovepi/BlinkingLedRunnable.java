package eu.arrowhead.demo.grovepi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.devices.GroveLed;

public class BlinkingLedRunnable implements Runnable {

  private final Logger logger = LogManager.getLogger();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ExecutorService executorService;
  private final GroveLed led;
  private boolean state = false;

  public BlinkingLedRunnable(final ExecutorService executorService, final GroveLed led) {
    this.executorService = executorService;
    this.led = led;
  }

  public synchronized void start() {
    if (running.compareAndExchange(false, true)) {
      executorService.submit(this);
    }
  }

  public synchronized void stop() {
    running.set(false);
  }

  @Override
  public void run() {
    while (running.get()) {
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