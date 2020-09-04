package eu.arrowhead.client.plc;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.demo.hk2.JacksonJsonProviderAtRest;
import eu.arrowhead.demo.web.HttpServerCustomizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@SpringBootApplication(scanBasePackages = CommonConstants.BASE_PACKAGE)
@SpringBootConfiguration
@EnableConfigurationProperties({ServerProperties.class})
public class PlcMain implements Runnable {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    private ExecutorService executorService;

    public static void main(String[] args) throws InterruptedException {
        final ConfigurableApplicationContext ctx = SpringApplication.run(PlcMain.class);
        Thread.sleep(2000);
    }

    @EventListener(ApplicationFailedEvent.class)
    public void failed(final ApplicationFailedEvent event) {
        SpringApplication.exit(event.getApplicationContext());
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public HttpServerCustomizer httpServerCustomizer() {
        return (server) -> server.registerClasses(PlcController.class, JacksonJsonProviderAtRest.class);
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
                logger.warn(e.getMessage(), e);
            }
        }
    }
}
