package org.iot.raspberry.grovepi.devices;

import static org.iot.raspberry.grovepi.GrovePiCommands.aWrite_cmd;
import static org.iot.raspberry.grovepi.GrovePiCommands.dWrite_cmd;
import static org.iot.raspberry.grovepi.GrovePiCommands.pMode_cmd;
import static org.iot.raspberry.grovepi.GrovePiCommands.pMode_out_arg;
import static org.iot.raspberry.grovepi.GrovePiCommands.unused;

import java.io.IOException;
import org.iot.raspberry.grovepi.GroveDigitalPin;
import org.iot.raspberry.grovepi.GroveIO;
import org.iot.raspberry.grovepi.GrovePi;

@GroveDigitalPin
public class GroveLed {

  public static int MAX_BRIGTHNESS = 255;

  private final GrovePi grovePi;
    protected final int pin;

  public GroveLed(GrovePi grovePi, int pin) throws IOException {
    this.grovePi = grovePi;
    this.pin = pin;
    grovePi.execVoid((GroveIO io) -> io.write(pMode_cmd, pin, pMode_out_arg, unused));
    set(false);
  }

  public void set(boolean value) throws IOException {
    int val = value ? 1 : 0;
    grovePi.execVoid((GroveIO io) -> io.write(dWrite_cmd, pin, val, unused));
  }

  public void set(int value) throws IOException {
    final int val = Math.max(0, Math.min(value, MAX_BRIGTHNESS));
    grovePi.execVoid((GroveIO io) -> io.write(aWrite_cmd, pin, val, unused));
  }

}
