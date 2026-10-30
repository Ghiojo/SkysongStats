package org.skysongdev.skysongstats.Utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.skysongdev.skysongstats.SkysongStats;

import java.sql.SQLException;

public class Utils {
    private final SkysongStats plugin;
    private static final MiniMessage mm = MiniMessage.miniMessage();
    public static final String PLUGIN_TAG = "<dark_gray>[<gold>SkysongStats<dark_gray>] ";
    public static NamespacedKey statlineKey = new NamespacedKey(SkysongStats.getPlugin(), "id");
    public static NamespacedKey statButtonKey = new NamespacedKey(SkysongStats.getPlugin(), "stat");


    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public CharacterManager getCharacterManager() {
        return characterManager;
    }

    private final ProfileManager profileManager = new ProfileManager();
    private final CharacterManager characterManager = new CharacterManager();

    public Utils(SkysongStats plugin){
        this.plugin = plugin;
    }
    public static MiniMessage getMiniMessage(){ return mm; }

    public boolean isPlayerInDatabase(String uuid) throws SQLException{
        return plugin.getDatabase().checkForUUID(uuid);
    }


}
