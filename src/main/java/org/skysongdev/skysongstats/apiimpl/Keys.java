package org.skysongdev.skysongstats.apiimpl;

import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.storage.ProfileKey;

// Converts between the API's CharacterId and the internal ProfileKey, so the
// internal one never reaches another plugin.
final class Keys {
    private Keys() {
    }

    static CharacterId toApi(ProfileKey key) {
        return new CharacterId(key.player(), key.profile());
    }

    static ProfileKey toInternal(CharacterId character) {
        return new ProfileKey(character.player(), character.profile());
    }
}
