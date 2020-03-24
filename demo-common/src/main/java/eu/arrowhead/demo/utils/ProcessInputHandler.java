package eu.arrowhead.demo.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;

public class ProcessInputHandler {

    private final ExecutorService executorService;
    private final AtomicReference<Future<?>> reference = new AtomicReference<>();
    private Consumer<String> consumer;

    public ProcessInputHandler(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    public synchronized void start() {
        if (reference.get() == null) {
            final Future<?> future = executorService.submit(new StreamGobbler(System.in, consumer));
            reference.set(future);
        }
    }

    @PreDestroy
    public synchronized void stop() {
        if (reference.getAndSet(null) != null) {
            final Future<?> future = reference.get();
            future.cancel(true);
        }
    }

    public void setConsumer(final Consumer<String> consumer) {
        this.consumer = consumer;
    }
}
