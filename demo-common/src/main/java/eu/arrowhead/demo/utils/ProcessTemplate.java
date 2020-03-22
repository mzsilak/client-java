package eu.arrowhead.demo.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.util.Assert;

public class ProcessTemplate {

    private final static Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
    private final ProcessBuilder builder;

    private Path directory;
    private Executor executor;
    private Consumer<String> in;
    private Consumer<String> err;

    private Class<Object> gobbler;

    public ProcessTemplate(final String... args) {
        Assert.notEmpty(args, "Template must have a command");
        final String[] command = new String[args.length + 2];
        if (SystemUtils.IS_OS_LINUX) {
            command[0] = "sh";
            command[1] = "-c";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            command[0] = "cmd.exe";
            command[1] = "/c";
        }
        System.arraycopy(args, 0, command, 2, args.length);
        builder = new ProcessBuilder(command);
        executor = DEFAULT_EXECUTOR;
    }

    public Process execute() throws IOException {
        return builder.start();
    }

    public Process executeWithGobblers() throws IOException {
        final Process process = builder.start();
        alterStream(process.getInputStream(), in);
        alterStream(process.getErrorStream(), err);
        return process;
    }

    private void alterStream(final InputStream inputStream, final Consumer<String> consumer) {
        if (Objects.isNull(consumer)) {
            executor.execute(new NullStreamGobbler(inputStream));
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

    public void setInputStreamConsumer(final Consumer<String> in) {
        this.in = in;
    }

    public void setErrStreamConsumer(final Consumer<String> err) {
        this.err = err;
    }
}
