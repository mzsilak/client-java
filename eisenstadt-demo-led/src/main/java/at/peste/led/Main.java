package at.peste.led;


import eu.arrowhead.demo.grovepi.ControllableLed;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@SpringBootConfiguration
public class Main {

    public static void main(String[] args) throws InterruptedException {
        final ConfigurableApplicationContext ctx = SpringApplication.run(Main.class);
        final ControllableLed led = ctx.getBean(ControllableLed.class);
        led.blink();
        Thread.sleep(10000L);
        led.turnOff();
    }

    @Bean
    public GrovePi grovePi() throws IOException {
        return new GrovePi4J();
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean("redLed")
    public GroveLed redLed(final GrovePi grovePi) throws IOException {
        return new GroveLed(grovePi, 4);
    }

    @Bean("redControl")
    public ControllableLed blinkingRedLed(final ExecutorService executorService,
                                          @Qualifier("redLed") final GroveLed led) {
        return new ControllableLed(executorService, led);
    }
}

