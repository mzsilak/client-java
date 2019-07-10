package eu.arrowhead.demo.model;

import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.GrovePiSequence;
import org.iot.raspberry.grovepi.GrovePiSequenceVoid;
import org.iot.raspberry.grovepi.devices.GroveRgbLcd;

import java.io.IOException;

public class FakeGrovePi implements GrovePi {

    @Override
    public GroveRgbLcd getLCD() throws IOException {
        return null;
    }

    @Override
    public <T> T exec(GrovePiSequence<T> sequence) throws IOException {
        return null;
    }

    @Override
    public void execVoid(GrovePiSequenceVoid sequence) throws IOException {

    }

    @Override
    public void close() {

    }
}
