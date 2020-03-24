package eu.arrowhead.demo.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Assert;

public class ProcessTemplate {

    private final static Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
    private final Logger logger = LogManager.getLogger();
    private final ProcessBuilder builder;

    private Executor executor = DEFAULT_EXECUTOR;
    private Consumer<String> inputStreamConsumer;
    private boolean manualInStream = false;

    public ProcessTemplate(final String... args) {
        Assert.notEmpty(args, "Template must have a command");
        builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
    }

    public Process execute() throws IOException {
        logger.debug("Starting new process: {}", builder.command());
        final Process process = builder.start();
        if (!manualInStream) {
            alterStream(process.getInputStream(), inputStreamConsumer);
        }
        return process;
    }

    private void alterStream(final InputStream inputStream, final Consumer<String> consumer) {
        if (Objects.isNull(consumer)) {
            executor.execute(new LoggingStreamGobbler(inputStream, Level.DEBUG, "IGNORED: "));
        } else {
            executor.execute(new StreamGobbler(inputStream, consumer));
        }
    }

    public void startGobbler(final StreamGobbler gobbler) {
        executor.execute(gobbler);
    }

    public void directory(final File directory) {
        builder.directory(directory);
    }

    public void executor(final Executor executor) {
        Assert.notNull(executor, "Executor must not be null");
        this.executor = executor;
    }

    public void setInputStreamConsumer(final Consumer<String> inputStreamConsumer) {
        this.inputStreamConsumer = inputStreamConsumer;
    }

    public void manualInputStream(boolean manualInStream) {
        this.manualInStream = manualInStream;
    }
}
