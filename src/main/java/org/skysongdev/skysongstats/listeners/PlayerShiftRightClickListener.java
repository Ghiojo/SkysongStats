package org.skysongdev.skysongstats.listeners;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.skysongdev.skysongstats.commands.character.ViewCharacter;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class PlayerShiftRightClickListener implements Listener {
    @EventHandler
    public void onPlayerShiftRightClick(PlayerInteractEntityEvent event) {
        if(event.getRightClicked() instanceof Player){
            if(!event.getPlayer().isSneaking()) return;
            if(event.isCancelled()) return;

            event.setCancelled(true);

            Player player = event.getPlayer();
            Player target = (Player) event.getRightClicked();
            ViewCharacter viewCharacter = new ViewCharacter();
            Command command = Bukkit.getPluginCommand("schar");
            viewCharacter.onCommand(player, command, "character", new String[]{target.getName()});
        }
    }
}
