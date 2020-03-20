package eu.arrowhead.demo.grovepi.mocks;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;

public class FakeLed extends GroveLed {

  private final Logger logger = LogManager.getLogger();
  private final int pin;

  public FakeLed(GrovePi grovePi, int pin) throws IOException {
    super(grovePi, pin);
    this.pin = pin;
  }

  @Override
  public void set(int value) throws IOException {
    final double val = (Math.max(0, Math.min(value, MAX_BRIGTHNESS)) * 100.0) / MAX_BRIGTHNESS;
    logger.info("Setting brightness to {}% at pin {}", val, pin);
  }

  @Override
  public void set(boolean value) throws IOException {
    String val = value ? "ON" : "OFF";
    logger.info("Turning led {} at pin {}", val, pin);
  }
}
