package org.skysongdev.skysongstats.skill;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.node.types.PermissionNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LuckPermsNodes} against a stand-in LuckPerms API built from proxies: the
 * real one only exists inside a running LuckPerms plugin.
 */
class LuckPermsNodesTest {
    private static final UUID ALICE = UUID.randomUUID();

    private final List<String> calls = new ArrayList<>();
    private UUID modified;
    private CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(LuckPermsNodesTest.class.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @SuppressWarnings("unchecked")
    private LuckPerms luckPerms() {
        NodeMap data = proxy(NodeMap.class, (self, method, args) -> {
            calls.add(method.getName() + " " + ((Node) args[0]).getKey());
            return null;
        });
        User user = proxy(User.class, (self, method, args) -> data);
        UserManager users = proxy(UserManager.class, (self, method, args) -> {
            modified = (UUID) args[0];
            ((Consumer<User>) args[1]).accept(user);
            return result;
        });
        NodeBuilderRegistry builders = proxy(NodeBuilderRegistry.class, (self, method, args) -> permissionBuilder());
        return proxy(LuckPerms.class, (self, method, args) ->
                "getUserManager".equals(method.getName()) ? users : builders);
    }

    // A PermissionNode.Builder that only remembers the permission it is given.
    private static PermissionNode.Builder permissionBuilder() {
        String[] permission = new String[1];
        return proxy(PermissionNode.Builder.class, (self, method, args) -> {
            if ("permission".equals(method.getName())) {
                permission[0] = (String) args[0];
                return self;
            }
            return proxy(PermissionNode.class, (node, nodeMethod, nodeArgs) -> permission[0]);
        });
    }

    @Test
    void addsAndRemovesTheNodesOnThePlayersUser() {
        new LuckPermsNodes(luckPerms(), error -> { }).update(ALICE,
                Set.of("skysongstats.skill.tande.novice"), Set.of("skysongstats.skill.tande.expert"));
        assertEquals(ALICE, modified);
        assertEquals(List.of("add skysongstats.skill.tande.novice", "remove skysongstats.skill.tande.expert"), calls);
    }

    @Test
    void aFailedSaveIsReported() {
        List<Throwable> failures = new ArrayList<>();
        result = CompletableFuture.failedFuture(new IllegalStateException("storage down"));
        new LuckPermsNodes(luckPerms(), failures::add).update(ALICE, Set.of(), Set.of());
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().getMessage().contains("storage down"), failures.toString());
    }
}
