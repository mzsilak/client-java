package eu.arrowhead.demo;

import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;
import org.iot.raspberry.grovepi.devices.GroveRotarySensor;
import org.iot.raspberry.grovepi.devices.GroveRotaryValue;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import java.io.IOException;

public class DemoMain
{

    private final GrovePi grovePi;
    private final GroveDigitalIn button;
    private final GroveLed red;
    private final GroveLed green;
    private final GroveLed blue;
    private final GroveRotarySensor rotary;

    private double previousRotaryValue;
    private boolean toogle;

    //public static void main(final String[] args) throws IOException {
    //    DemoMain main = new DemoMain();
    //}

    public DemoMain() throws IOException {
        this.grovePi = new GrovePi4J();
        rotary = new GroveRotarySensor(grovePi, 1); // A1
        red = new GroveLed(grovePi, 3); // D3
        green = new GroveLed(grovePi, 4); // D4
        blue = new GroveLed(grovePi, 5); // D5
        button =  grovePi.getDigitalIn(6); // D6

        System.out.println("Setting initial values");
        red.set(true);
        green.set(false);
        blue.set(false);

        setLedToRotary(rotary.get());

        System.out.println("Setting button listener");

        button.setListener((boolean oldValue, boolean newValue) -> {
            try {
                System.out.println("Button pressed");

                red.set(oldValue);
                green.set(newValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        final Thread listenerThread = new Thread(button);
        listenerThread.setDaemon(true);
        listenerThread.start();

        System.out.println("Entering loop");

        while(true)
        {
            try {
                Thread.sleep(500L);
                setLedToRotary(rotary.get());
                button.get(); // automatically calls listener
                toogle();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean setRotaryValue(final double newValue)
    {
        // normalize values
        double value = Math.floor((newValue * 100)) / 100D;
        boolean retValue = previousRotaryValue != value;
        previousRotaryValue = value;
        return retValue;
    }

    private void toogle() throws IOException {
        toogle = !toogle;
        System.out.println("Setting red to " + toogle);
        red.set(toogle);
    }

    private void setLedToRotary(GroveRotaryValue value) throws IOException {
        if(setRotaryValue(value.getFactor()))
        {
            int val = (int) (GroveLed.MAX_BRIGTHNESS * value.getFactor());
            System.out.println("New value detected: " + val);
            blue.set(val);
        }
    }
}
