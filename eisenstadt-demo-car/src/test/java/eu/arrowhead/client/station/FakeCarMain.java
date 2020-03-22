package eu.arrowhead.client.station;

import eu.arrowhead.client.car.ElectricCarController;
import eu.arrowhead.client.car.ElectricCarService;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.demo.grovepi.GroveButtonObserver;
import eu.arrowhead.demo.grovepi.mocks.FakeButtonObserver;
import eu.arrowhead.demo.grovepi.mocks.FakeGrovePI;
import eu.arrowhead.demo.grovepi.mocks.FakeLed;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;
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
public class FakeCarMain {

    public static void main(String[] args) {
        final ConfigurableApplicationContext ctx = SpringApplication.run(FakeCarMain.class);
        ElectricCarController.carService = ctx.getBean(ElectricCarService.class);
    }

    @Bean
    public GrovePi grovePi() throws IOException {
        return new FakeGrovePI();
    }

    @Bean("button")
    public GroveButtonObserver buttonObserver(final ExecutorService executorService, final GrovePi grovePi,
                                              @Value("${server.button.pin}") final int pin) throws IOException {
        return new FakeButtonObserver(executorService, grovePi, pin);
    }

    @Bean("greenLed")
    public GroveLed greenLed(final GrovePi grovePi, @Value("${server.green.pin}") final int pin) throws IOException {
        return new FakeLed(grovePi, pin);
    }

    @Bean("redLed")
    public GroveLed redLed(final GrovePi grovePi, @Value("${server.green.pin}") final int pin) throws IOException {
        return new FakeLed(grovePi, pin);
    }
}
