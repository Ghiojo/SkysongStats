package org.skysongdev.skysongstats.service.downtime;

/** A {@link DowntimeRepository} could not read or write. */
public class DowntimeStorageException extends RuntimeException {
    public DowntimeStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
