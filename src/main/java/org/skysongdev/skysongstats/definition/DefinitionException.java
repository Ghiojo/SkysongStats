package org.skysongdev.skysongstats.definition;

import java.util.List;

// Thrown when a definition file (stats.yml, ...) has mistakes. It carries every
// mistake found, so the server owner can fix them all in one go.
public class DefinitionException extends RuntimeException {
    private final transient List<String> errors;

    public DefinitionException(String file, List<String> errors) {
        super(file + " has " + errors.size() + " error(s):\n - " + String.join("\n - ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
