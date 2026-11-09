package org.skysongdev.skysongstats.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.importer.ChatTweaksImporter;
import org.skysongdev.skysongstats.messages.Messages;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

// /ssimport chattweaks: the one-off import from SkysongChatTweaks. The command's
// permission (skysongstats.import, in plugin.yml) is checked by the server.
public final class ImportCommand implements TabExecutor {
    private static final String CHATTWEAKS = "chattweaks";

    private final ChatTweaksImporter chatTweaks;
    private final Supplier<Messages> messages;

    public ImportCommand(ChatTweaksImporter chatTweaks, Supplier<Messages> messages) {
        this.chatTweaks = Objects.requireNonNull(chatTweaks, "chatTweaks");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 1 && CHATTWEAKS.equals(args[0].toLowerCase(Locale.ROOT))) {
            chatTweaks.start(sender);
        } else {
            messages.get().send(sender, "import.usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        if (args.length == 1 && CHATTWEAKS.startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of(CHATTWEAKS);
        }
        return List.of();
    }
}
