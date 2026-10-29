package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongstats.commands.stat.MessageSink;
import org.skysongdev.skysongstats.setup.MenuTexts;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

// Hands the plugin's messages.yml to the stat commands and the /setup screen.
// /ssreload replaces the Messages, so it is looked up on every use.
public final class PluginTexts implements MessageSink, MenuTexts {
    private final Supplier<Messages> messages;

    public PluginTexts(Supplier<Messages> messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @Override
    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        messages.get().send(to, key, placeholders);
    }

    @Override
    public boolean has(String key) {
        return messages.get().has(key);
    }

    @Override
    public List<String> lines(String key) {
        return messages.get().rawLines(key);
    }

    @Override
    public Component line(String key, Map<String, String> placeholders) {
        return messages.get().get(key, placeholders);
    }

    @Override
    public List<Component> lines(String key, Map<String, String> placeholders) {
        return messages.get().getLines(key, placeholders);
    }
}
