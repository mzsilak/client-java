package eu.arrowhead.demo.grovepi.mocks;

import java.io.IOException;
import org.iot.raspberry.grovepi.GrovePiSequence;
import org.iot.raspberry.grovepi.GrovePiSequenceVoid;
import org.iot.raspberry.grovepi.devices.GroveRgbLcd;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

public class FakeGrovePI extends GrovePi4J {

  public FakeGrovePI() throws IOException {
    super();
  }

  @Override
  public <T> T exec(GrovePiSequence<T> sequence) throws IOException {
    return null;
  }

  @Override
  public void execVoid(GrovePiSequenceVoid sequence) throws IOException {
    // empty
  }

  @Override
  public void close() {
    // empty
  }

  @Override
  public GroveRgbLcd getLCD() throws IOException {
    throw new UnsupportedOperationException();
  }
}
