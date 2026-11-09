package org.skysongdev.skysongstats.importer;

import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.storage.LearnSource;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

// Turns SkysongChatTweaks' profile rows into known injuries. Each permission node
// is matched back to the injuries whose permissions: list held it, and the
// character learns them with LearnSource.IMPORT: no event, no announcement.
// Known injuries are never duplicated, so a second run only counts them as
// already known. Every row or node that can't be imported is counted and gets a
// console line saying why.
final class KnownInjuryImport {
    record Result(int characters, int learnt, int alreadyKnown, int unknownProfiles, int unreadableRows,
                  int unmatchedNodes, List<String> console) {
        Result {
            console = List.copyOf(console);
        }
    }

    private final AfflictionService afflictions;
    // node (lowercase) -> the SkysongChatTweaks injury ids that listed it
    private final Map<String, List<String>> injuriesByNode;
    private final Predicate<ProfileKey> characterExists;
    private final List<String> console = new ArrayList<>();
    private int characters;
    private int learnt;
    private int alreadyKnown;
    private int unknownProfiles;
    private int unreadableRows;
    private int unmatchedNodes;

    private KnownInjuryImport(AfflictionService afflictions, Map<String, List<String>> injuriesByNode,
                              Predicate<ProfileKey> characterExists) {
        this.afflictions = afflictions;
        this.injuriesByNode = injuriesByNode;
        this.characterExists = characterExists;
    }

    static Result run(List<ChatTweaksProfiles.Row> rows, Map<String, List<String>> injuriesByNode,
                      Predicate<ProfileKey> characterExists, AfflictionService afflictions) {
        Objects.requireNonNull(rows, "rows");
        KnownInjuryImport run = new KnownInjuryImport(Objects.requireNonNull(afflictions, "afflictions"),
                Objects.requireNonNull(injuriesByNode, "injuriesByNode"),
                Objects.requireNonNull(characterExists, "characterExists"));
        rows.forEach(run::importRow);
        return new Result(run.characters, run.learnt, run.alreadyKnown, run.unknownProfiles, run.unreadableRows,
                run.unmatchedNodes, run.console);
    }

    private void importRow(ChatTweaksProfiles.Row row) {
        key(row).ifPresentOrElse(character -> importCharacter(character, row), () -> {
            unreadableRows++;
            console.add("Skipped row " + row.uuid() + " / " + row.profile() + ": not a player UUID and profile");
        });
    }

    private void importCharacter(ProfileKey character, ChatTweaksProfiles.Row row) {
        if (!characterExists.test(character)) {
            unknownProfiles++;
            console.add("Skipped " + describe(row) + ": SkysongStats has no such character");
            return;
        }
        int before = learnt;
        row.nodes().forEach(node -> importNode(character, row, node));
        if (learnt > before) {
            characters++;
        }
    }

    private void importNode(ProfileKey character, ChatTweaksProfiles.Row row, String node) {
        List<String> injuries = injuriesByNode.getOrDefault(node.toLowerCase(Locale.ROOT), List.of());
        if (injuries.isEmpty()) {
            unmatchedNodes++;
            console.add("Skipped " + node + " of " + describe(row) + ": no injury in SkysongChatTweaks lists it");
        }
        for (String injury : injuries) {
            learn(character, row, node, injury);
        }
    }

    private void learn(ProfileKey character, ChatTweaksProfiles.Row row, String node, String injury) {
        switch (afflictions.discover(character, injury, LearnSource.IMPORT)) {
            case LEARNT -> {
                learnt++;
                console.add(describe(row) + " knows " + injury);
            }
            case ALREADY_KNOWN -> alreadyKnown++;
            default -> {
                unmatchedNodes++;
                console.add("Skipped " + node + " of " + describe(row) + ": injury " + injury
                        + " is not in injuries.yml");
            }
        }
    }

    private static Optional<ProfileKey> key(ChatTweaksProfiles.Row row) {
        if (row.uuid() == null || row.profile() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ProfileKey(UUID.fromString(row.uuid().trim()), row.profile()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String describe(ChatTweaksProfiles.Row row) {
        return row.uuid() + " (" + row.profile() + ")";
    }
}
