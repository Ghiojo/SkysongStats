package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.api.event.ChangeCause;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

// What the change being made right now is caused by, for the change events. A
// change made inside during(cause, ...) carries that cause; anything else is a
// command. Changes only happen on the main thread, so one stack is enough.
public final class ChangeCauses {
    private final Deque<ChangeCause> active = new ArrayDeque<>();

    public ChangeCause current() {
        ChangeCause cause = active.peek();
        return cause == null ? ChangeCause.COMMAND : cause;
    }

    public <T> T during(ChangeCause cause, Supplier<T> change) {
        active.push(cause);
        try {
            return change.get();
        } finally {
            active.pop();
        }
    }

    public void during(ChangeCause cause, Runnable change) {
        during(cause, () -> {
            change.run();
            return null;
        });
    }
}
