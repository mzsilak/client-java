package org.iot.raspberry.grovepi;

import static org.iot.raspberry.grovepi.GrovePiCommands.dRead_cmd;
import static org.iot.raspberry.grovepi.GrovePiCommands.pMode_cmd;
import static org.iot.raspberry.grovepi.GrovePiCommands.pMode_in_arg;
import static org.iot.raspberry.grovepi.GrovePiCommands.unused;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroveDigitalIn implements Runnable {

    private final GrovePi grovePi;
    private final int pin;
    private boolean status = false;
    private GroveDigitalInListener listener;

    public GroveDigitalIn(GrovePi grovePi, int pin) throws IOException {
        this.grovePi = grovePi;
        this.pin = pin;
        grovePi.execVoid((GroveIO io) -> io.write(pMode_cmd, pin, pMode_in_arg, unused));
    }

    public void setListener(GroveDigitalInListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            get();
        } catch (IOException ex) {
            Logger.getLogger("GrovePi").log(Level.SEVERE, null, ex);
        }
    }

    public boolean get() throws IOException {
        boolean st = grovePi.exec((GroveIO io) -> {
            io.write(dRead_cmd, pin, unused, unused);
            io.sleep(100);
            return io.read() == 1;
        });
        if (listener != null && status != st) {
            listener.onChange(status, st);
        }
        this.status = st;
        return st;
    }

}
