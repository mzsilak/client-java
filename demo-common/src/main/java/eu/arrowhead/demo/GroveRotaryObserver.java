package eu.arrowhead.demo;

import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveInputDeviceListener;
import org.iot.raspberry.grovepi.devices.GroveRotarySensor;
import org.iot.raspberry.grovepi.devices.GroveRotaryValue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroveRotaryObserver implements Runnable, GroveInputDeviceListener<GroveRotaryValue> {

    private final GrovePi grovePi;
    private final GroveRotarySensor rotary;
    private final AtomicBoolean running;
    private RotaryValueListener listener = null;
    private double value;

    public GroveRotaryObserver(final GrovePi grovePi) throws IOException {
        this.grovePi = grovePi;
        rotary = new GroveRotarySensor(grovePi, 1); // A1
        rotary.setListener(this);
        running = new AtomicBoolean(true);
    }

    public void stop()
    {
        running.set(false);
    }

    @Override
    public void run() {
        while(running.get())
        {
            try {
                rotary.get();
                Thread.sleep(200L);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setListener(final RotaryValueListener listener) {
        this.listener = listener;
    }

    @Override
    public void onChange(final GroveRotaryValue groveRotaryValue) {
        double newValue = normalize(groveRotaryValue.getFactor());
        if(listener == null)
        {
            return;
        }
        if(value != newValue)
        {
            value = newValue;
            listener.rotaryValueChanged(newValue);
        }
    }

    private double normalize(final double value)
    {
        return Math.floor(value * 100) / 100;
    }
}
