package eu.arrowhead.demo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StreamGobbler implements Runnable {

    private final Logger logger = LogManager.getLogger();
    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(final InputStream inputStream, final Consumer<String> consumer) {
        logger.info("Created new {} for consumer {}", getClass().getSimpleName(), consumer);
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        logger.info("{} running for {}", getClass().getSimpleName(), consumer);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    logger.debug("Reading line: '{}'", line);
                    consumer.accept(line.trim());
                } catch (final Exception e) {
                    logger.warn("StreamGobbler consumer messed up: '{}'", e.getMessage());
                }
            }
        } catch (final IOException e) {
            logger.error("StreamGobbler failed with: {}", e.getMessage());
        }
        logger.info("{} finished for {}", getClass().getSimpleName(), consumer);
    }
}