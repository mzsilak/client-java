package eu.arrowhead.demo;

import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GroveDigitalInListener;
import org.iot.raspberry.grovepi.GrovePi;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroveButtonObserver implements Runnable, GroveDigitalInListener {

    private final GrovePi grovePi;
    private final GroveDigitalIn button;
    private final AtomicBoolean running;
    private ButtonPressedListener listener = null;


    public GroveButtonObserver(GrovePi grovePi) throws IOException {
        this.grovePi = grovePi;
        button = grovePi.getDigitalIn(6); // D6
        button.setListener(this);
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
                button.get();
                Thread.sleep(200L);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setListener(ButtonPressedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onChange(boolean oldValue, boolean newValue)
    {
        if(listener == null)
        {
            return;
        }
        if(!oldValue && newValue)
        {
            listener.trigger();
        }
    }
}
