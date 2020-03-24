package eu.arrowhead.demo.utils;

import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NullStreamGobbler extends StreamGobbler {

    private final Logger logger = LogManager.getLogger();

    public NullStreamGobbler(final InputStream inputStream) {
        super(inputStream, (s) -> {
            // empty
        });

        logger.info("Created new NullStreamGobbler");
    }
}