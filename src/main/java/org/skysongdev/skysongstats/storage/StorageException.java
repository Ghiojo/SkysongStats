package org.skysongdev.skysongstats.storage;

// A database read or write failed.
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
