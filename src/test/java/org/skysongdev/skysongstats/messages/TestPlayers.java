package org.skysongdev.skysongstats.messages;

import org.bukkit.OfflinePlayer;

import java.lang.reflect.Proxy;

// A stand-in OfflinePlayer that only knows its name, for tests without a server.
final class TestPlayers {
    private TestPlayers() {
    }

    static OfflinePlayer offline(String name) {
        return (OfflinePlayer) Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "hashCode" -> name.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
