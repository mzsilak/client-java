package eu.arrowhead.client.station;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.demo.grovepi.mocks.FakeGrovePI;
import eu.arrowhead.demo.grovepi.mocks.FakeLed;
import java.io.IOException;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;
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
public class FakeStationMain {

    public static void main(String[] args) {
        final ConfigurableApplicationContext ctx = SpringApplication.run(FakeStationMain.class);
        ChargingStationController.chargingService = ctx.getBean(ChargingStationService.class);
    }

    @Bean
    public GrovePi grovePi() throws IOException {
        return new FakeGrovePI();
    }

    @Bean("greenLed")
    public GroveLed greenLed(final GrovePi grovePi) throws IOException {
        return new FakeLed(grovePi, 0);
    }

    @Bean("redLed")
    public GroveLed redLed(final GrovePi grovePi) throws IOException {
        return new FakeLed(grovePi, 1);
    }
}
