package eu.arrowhead.client.station;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.grovepi.mocks.FakeGrovePI;
import eu.arrowhead.demo.utils.ProcessTemplate;
import eu.arrowhead.demo.web.HttpServerCustomizer;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;
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
public class StationMain {

    public static void main(String[] args) {
        final ConfigurableApplicationContext ctx = SpringApplication.run(StationMain.class);
        ChargingStationController.chargingService = ctx.getBean(ChargingStationService.class);
    }

    @Bean
    public GrovePi grovePi() throws IOException {
        return new FakeGrovePI();
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean("greenLed")
    public GroveLed greenLed(final GrovePi grovePi) throws IOException {
        return new GroveLed(grovePi, 0);
    }

    @Bean("redLed")
    public GroveLed redLed(final GrovePi grovePi) throws IOException {
        return new GroveLed(grovePi, 1);
    }

    @Bean("greenControl")
    public ControllableLed blinkingGreenLed(final ExecutorService executorService,
                                            @Qualifier("greenLed") final GroveLed led) {
        return new ControllableLed(executorService, led);
    }

    @Bean("redControl")
    public ControllableLed blinkingRedLed(final ExecutorService executorService,
                                          @Qualifier("redLed") final GroveLed led) {
        return new ControllableLed(executorService, led);
    }

    @Bean("rfid")
    public ProcessTemplate rfidProcess(final ExecutorService executorService,
                                       @Value("${command.rfid}") final String rfid) {
        final ProcessTemplate template = new ProcessTemplate(rfid.split("\\P{L}+"));
        template.executor(executorService);
        return template;
    }

    @Bean("powerOn")
    public ProcessTemplate powerOnProcess(final ExecutorService executorService,
                                          @Value("${command.power.on}") final String power) {
        final ProcessTemplate template = new ProcessTemplate(power.split("\\P{L}+"));
        template.executor(executorService);
        return template;

    }

    @Bean("powerOff")
    public ProcessTemplate powerOffProcess(final ExecutorService executorService,
                                           @Value("${command.power.off}") final String power) {
        final ProcessTemplate template = new ProcessTemplate(power.split("\\P{L}+"));
        template.executor(executorService);
        return template;

    }

    @Bean("powerRead")
    public ProcessTemplate powerReadProcess(final ExecutorService executorService,
                                            @Value("${command.power.status}") final String power) {
        final ProcessTemplate template = new ProcessTemplate(power.split("\\P{L}+"));
        template.executor(executorService);
        return template;

    }

    @Bean
    public HttpServerCustomizer httpServerCustomizer() {
        return (server) -> server.registerPackages(CommonConstants.BASE_PACKAGE);
    }
}
