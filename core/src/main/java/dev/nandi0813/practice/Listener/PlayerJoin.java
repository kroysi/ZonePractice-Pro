package dev.nandi0813.practice.Listener;

import dev.nandi0813.practice.Manager.Backend.ConfigManager;
import dev.nandi0813.practice.Manager.Inventory.InventoryManager;
import dev.nandi0813.practice.Manager.PlayerDisplay.Nametag.NametagManager;
import dev.nandi0813.practice.Manager.Profile.Enum.ProfileStatus;
import dev.nandi0813.practice.Manager.Profile.Profile;
import dev.nandi0813.practice.Manager.Profile.ProfileManager;
import dev.nandi0813.practice.Manager.Sidebar.SidebarManager;
import dev.nandi0813.practice.Util.PermanentConfig;
import dev.nandi0813.practice.Util.PlayerUtil.PlayerUtil;
import dev.nandi0813.practice.ZonePractice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoin implements Listener {

    @EventHandler ( priority = EventPriority.HIGHEST )
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();

        Profile profile = ProfileManager.getInstance().getProfile(uuid);
        if (profile == null)
            profile = ProfileManager.getInstance().newProfile(player, uuid);

        profile.checkGroup();

        // Send nametag teams
        NametagManager.getInstance().sendTeams(player);

        profile.setLastJoin(System.currentTimeMillis());

        // Check how many custom kits the player is allowed to save.
        int customKitPerm = profile.getCustomKitPerm();
        if (customKitPerm > 0) {
            profile.setAllowedCustomKits(customKitPerm);
        }

        // Set the lobby inventory
        if (PermanentConfig.JOIN_TELEPORT_LOBBY) {
            final Profile profile1 = profile;
            Bukkit.getScheduler().runTaskLater(ZonePractice.getInstance(), () ->
            {
                PlayerUtil.setPlayerWorldTime(player);

                if (ConfigManager.getBoolean("STAFF-MODE.JOIN-HIDE-FROM-PLAYERS") && player.hasPermission("zpp.staffmode"))
                    profile1.setHideFromPlayers(true);
            }, 10L);

            InventoryManager.getInstance().setLobbyInventory(player, true);
        } else {
            ProfileManager.getInstance().getProfile(player).setStatus(ProfileStatus.OFFLINE);
            SidebarManager.getInstance().unLoadSidebar(player);
        }
    }

}
