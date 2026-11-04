package org.skysongdev.skysongstats.apiimpl;

import java.util.function.BooleanSupplier;

// Guards the API methods that touch character data: the services behind them are
// not thread-safe, so a call from another thread is refused instead of racing them.
final class MainThread {
    private final BooleanSupplier isMainThread;

    MainThread(BooleanSupplier isMainThread) {
        this.isMainThread = isMainThread;
    }

    void check() {
        if (!isMainThread.getAsBoolean()) {
            throw new IllegalStateException("The SkysongStats API must be called on the server's main thread."
                    + " Use Bukkit.getScheduler().runTask(...) to get back to it.");
        }
    }
}
