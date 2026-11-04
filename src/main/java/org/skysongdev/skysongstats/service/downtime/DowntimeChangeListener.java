package org.skysongdev.skysongstats.service.downtime;

/**
 * Told about every downtime change. The plugin passes one that fires
 * {@link org.skysongdev.skysongstats.api.event.DowntimeChangeEvent}; tests pass a list.
 */
@FunctionalInterface
public interface DowntimeChangeListener {
    DowntimeChangeListener NONE = change -> { };

    void onChange(DowntimeChange change);
}
