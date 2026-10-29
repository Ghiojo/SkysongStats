package org.skysongdev.skysongstats.storage;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// Runs database writes on one background thread, one after another in the order
// they were submitted, so the server thread never waits on MySQL. A write that
// fails is handed to onFailure (the plugin logs it) and the next write still runs.
public class AsyncWriter implements AutoCloseable {
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    private final ExecutorService executor;
    private final Consumer<RuntimeException> onFailure;

    public AsyncWriter(Consumer<RuntimeException> onFailure) {
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SkysongStats database writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void submit(Runnable write) {
        Objects.requireNonNull(write, "write");
        try {
            executor.execute(() -> run(write));
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException("The database writer is closed", e);
        }
    }

    private void run(Runnable write) {
        try {
            write.run();
        } catch (RuntimeException e) {
            onFailure.accept(e);
        }
    }

    // Stops taking writes and waits for the queued ones to finish, for plugin
    // shutdown. Returns false if they did not finish within the timeout.
    public boolean close(Duration timeout) {
        executor.shutdown();
        try {
            return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() {
        close(SHUTDOWN_TIMEOUT);
    }
}
