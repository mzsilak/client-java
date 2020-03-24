package eu.arrowhead.client.car;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.grovepi.GroveButtonObserver;
import eu.arrowhead.demo.hk2.JacksonJsonProviderAtRest;
import eu.arrowhead.demo.web.HttpServerCustomizer;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = CommonConstants.BASE_PACKAGE)
@SpringBootConfiguration
@EnableConfigurationProperties({ServerProperties.class})
public class CarMain implements Runnable {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    private ExecutorService executorService;

    public static void main(String[] args) {
        final ConfigurableApplicationContext ctx = SpringApplication.run(CarMain.class);
        ElectricCarController.carService = ctx.getBean(ElectricCarService.class);
    }

    @Bean
    public GrovePi grovePi() throws IOException {
        return new GrovePi4J();
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean(value = "button", destroyMethod = "stop")
    public GroveButtonObserver buttonObserver(final ExecutorService executorService, final GrovePi grovePi,
                                              @Value("${server.button.pin}") final int pin) throws IOException {
        return new GroveButtonObserver(executorService, grovePi, pin);
    }

    @Bean("greenLed")
    public GroveLed greenLed(final GrovePi grovePi, @Value("${server.green.pin}") final int pin) throws IOException {
        return new GroveLed(grovePi, pin);
    }

    @Bean("redLed")
    public GroveLed redLed(final GrovePi grovePi, @Value("${server.red.pin}") final int pin) throws IOException {
        return new GroveLed(grovePi, pin);
    }

    @Bean(value = "greenControl", destroyMethod = "turnOff")
    public ControllableLed blinkingGreenLed(final ExecutorService executorService,
                                            @Qualifier("greenLed") final GroveLed led) {
        return new ControllableLed(executorService, led);
    }

    @Bean(value = "redControl", destroyMethod = "turnOff")
    public ControllableLed blinkingRedLed(final ExecutorService executorService,
                                          @Qualifier("redLed") final GroveLed led) {
        return new ControllableLed(executorService, led);
    }

    @Bean
    public HttpServerCustomizer httpServerCustomizer() {
        return (server) -> server.registerClasses(ElectricCarController.class, JacksonJsonProviderAtRest.class);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
    }

    @PostConstruct
    public void start() {
        running.set(true);
        executorService.execute(this);
    }

    @Override
    public void run() {
        logger.info("Entering main loop");
        while (running.get()) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // empty
            }
        }
    }
}
