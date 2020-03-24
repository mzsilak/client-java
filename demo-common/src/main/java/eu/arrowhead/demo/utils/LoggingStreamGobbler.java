package eu.arrowhead.demo.utils;

import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingStreamGobbler extends StreamGobbler {

    public LoggingStreamGobbler(final InputStream inputStream, final Level level) {
        super(inputStream, new LoggingConsumer(level, ""));
    }

    public LoggingStreamGobbler(final InputStream inputStream, final Level level, final String prefix) {
        super(inputStream, new LoggingConsumer(level, prefix));
    }

    private static class LoggingConsumer implements Consumer<String> {

        private final Logger logger = LogManager.getLogger();
        private final Level level;
        private final String prefix;

        public LoggingConsumer(Level level, String prefix) {
            this.level = level;
            this.prefix = prefix;
        }

        @Override
        public void accept(String s) {
            logger.log(level, "{} {}", prefix, s);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LoggingConsumer[");
            sb.append("level=").append(level);
            sb.append(", prefix='").append(prefix).append('\'');
            sb.append(']');
            return sb.toString();
        }
    }
}