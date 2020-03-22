package eu.arrowhead.demo.grovepi.mocks;

import java.io.IOException;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.GrovePiSequence;
import org.iot.raspberry.grovepi.GrovePiSequenceVoid;
import org.iot.raspberry.grovepi.devices.GroveRgbLcd;

public class FakeGrovePI implements GrovePi {

    public FakeGrovePI() throws IOException {
        super();
    }

    @Override
    public GroveRgbLcd getLCD() throws IOException {
        throw new UnsupportedOperationException();
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
}
