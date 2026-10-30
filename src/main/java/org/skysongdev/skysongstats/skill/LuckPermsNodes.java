package org.skysongdev.skysongstats.skill;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

// PlayerNodes on LuckPerms: loads the player's user (also when offline), changes
// its nodes and saves it, in the background. A failure is handed to onFailure.
public final class LuckPermsNodes implements PlayerNodes {
    private final LuckPerms luckPerms;
    private final Consumer<Throwable> onFailure;

    public LuckPermsNodes(LuckPerms luckPerms, Consumer<Throwable> onFailure) {
        this.luckPerms = Objects.requireNonNull(luckPerms, "luckPerms");
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
    }

    @Override
    public void update(UUID player, Set<String> grant, Set<String> revoke) {
        luckPerms.getUserManager().modifyUser(player, user -> {
            grant.forEach(node -> user.data().add(node(node)));
            revoke.forEach(node -> user.data().remove(node(node)));
        }).exceptionally(error -> {
            onFailure.accept(error);
            return null;
        });
    }

    private Node node(String permission) {
        return luckPerms.getNodeBuilderRegistry().forPermission().permission(permission).build();
    }
}
