package org.skysongdev.skysongstats.apiimpl;

import net.kyori.adventure.text.Component;
import org.skysongdev.skysongstats.api.SheetLayout;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Optional;

// Renders sheets for any character (see SheetRendering), for CharacterSheets.render.
interface SheetRenderer {
    Optional<List<Component>> render(ProfileKey character, SheetLayout layout, String name);

    Optional<List<Component>> render(ProfileKey character, List<String> lines, String name);
}
