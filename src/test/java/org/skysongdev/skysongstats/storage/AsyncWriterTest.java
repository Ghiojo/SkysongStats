package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link AsyncWriter}.
 */
class AsyncWriterTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Test
    void runsWritesInOrderOnItsOwnThread() {
        List<String> log = new CopyOnWriteArrayList<>();
        AsyncWriter writer = new AsyncWriter(e -> fail(e));
        for (int i = 0; i < 100; i++) {
            int index = i;
            writer.submit(() -> log.add(index + " " + Thread.currentThread().getName()));
        }
        assertTrue(writer.close(TIMEOUT));
        assertEquals(100, log.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i + " SkysongStats database writer", log.get(i));
        }
    }

    @Test
    void aFailedWriteIsReportedAndTheNextOneStillRuns() {
        List<RuntimeException> failures = new CopyOnWriteArrayList<>();
        List<String> done = new CopyOnWriteArrayList<>();
        AsyncWriter writer = new AsyncWriter(failures::add);
        writer.submit(() -> {
            throw new StorageException("boom", null);
        });
        writer.submit(() -> done.add("second"));
        assertTrue(writer.close(TIMEOUT));
        assertEquals(1, failures.size());
        assertEquals("boom", failures.getFirst().getMessage());
        assertEquals(List.of("second"), done);
    }

    @Test
    void closeWaitsForQueuedWrites() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        List<String> done = new CopyOnWriteArrayList<>();
        AsyncWriter writer = new AsyncWriter(e -> fail(e));
        writer.submit(() -> {
            started.countDown();
            sleep(200);
            done.add("slow");
        });
        writer.submit(() -> done.add("queued"));
        assertTrue(started.await(5, TimeUnit.SECONDS));
        writer.close();
        assertEquals(List.of("slow", "queued"), done);
    }

    @Test
    void closeReportsWritesThatDidNotFinishInTime() {
        CountDownLatch release = new CountDownLatch(1);
        AsyncWriter writer = new AsyncWriter(e -> fail(e));
        writer.submit(() -> await(release));
        assertFalse(writer.close(Duration.ofMillis(50)));
        release.countDown();
    }

    @Test
    void refusesWritesAfterClosing() {
        AsyncWriter writer = new AsyncWriter(e -> fail(e));
        writer.close();
        assertThrows(IllegalStateException.class, () -> writer.submit(() -> { }));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
