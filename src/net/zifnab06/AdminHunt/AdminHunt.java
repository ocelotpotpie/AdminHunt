package net.zifnab06.AdminHunt;

import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// ----------------------------------------------------------------------------------------------------------
/**
 * The main plugin class.
 */
public class AdminHunt extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------------------------------------
    /**
     * Stores the UUIDs of the players currently being hunted.
     */
    private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();

    // ------------------------------------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable().
     */
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();

        for (String uuidString : getConfig().getStringList("players")) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                ENABLED_PLAYERS.add(uuid);
                log("Loaded serialized player: " + uuidString);
            } catch (IllegalArgumentException e) {
                log("Invalid UUID found in config: " + uuidString);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onDisable().
     */
    @Override
    public void onDisable() {
        List<String> serializePlayers = ENABLED_PLAYERS.stream()
                                                       .map(UUID::toString)
                                                       .collect(Collectors.toList());
        getConfig().set("players", serializePlayers);
        saveConfig();
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Handles commands.
     *
     * @see JavaPlugin#onCommand(CommandSender, Command, String, String[]).
     */
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {

        if (command.getName().equalsIgnoreCase("adminhunt")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                return false;
            } else if (sender.hasPermission("adminhunt.toggle")) {
                String playerName = args[0];
                Player player = getServer().getPlayer(playerName);
                toggleHuntedStatus(player);
                return true;
            }
            return false;
        }

        if (command.getName().equalsIgnoreCase("adminhunt-list") && sender instanceof Player) {
            if (sender.hasPermission("adminhunt.toggle.list")) {
                String playerList = "Enabled players: ";
                if (ENABLED_PLAYERS.isEmpty()) {
                    playerList += "none!";
                } else {
                    playerList += ENABLED_PLAYERS.stream()
                                                 .map(getServer()::getPlayer)
                                                 .map(Player::getName)
                                                 .collect(Collectors.joining(", "));
                }
                sender.sendMessage(playerList);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Prevent WorldGuard from disabling PvP for hunted players, allowing admins to be attacked in PvE areas.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPVPDamage(DisallowedPVPEvent event) {
        Player defender = event.getDefender();
        Player attacker = event.getAttacker();
        if (isActive(defender) || isActive(attacker)) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * End the hunt for a player when they die.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (isActive(player)) {
            toggleHuntedStatus(player);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Toggle the player's hunted status on or off.
     *
     * @param player the player.
     */
    private void toggleHuntedStatus(Player player) {
        UUID uuid = player.getUniqueId();
        String msg;
        if (isActive(player)) {
            msg = ChatColor.GREEN + "[AdminHunt] The admin hunt has ended. " + player.getName()
                    + " has been found." + ChatColor.RESET;
            ENABLED_PLAYERS.remove(uuid);
        } else {
            msg = ChatColor.GREEN + "[AdminHunt] An admin hunt has begun! Find (and kill) "
                    + player.getName() + " as quickly as you can!" + ChatColor.RESET;
            ENABLED_PLAYERS.add(uuid);
        }
        getServer().broadcastMessage(msg);
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * Returns true if the player is being hunted.
     *
     * @param player the player.
     * @return true if the player is being hunted.
     */
    private boolean isActive(Player player) {
        return ENABLED_PLAYERS.contains(player.getUniqueId());
    }

    // ------------------------------------------------------------------------------------------------------
    /**
     * A logging convenience method, used instead of {@link java.util.logging.Logger} for colorizing this
     * plugin's name in console.
     *
     * @param message the message to log.
     */
    private static void log(String message) {
        System.out.println(PREFIX + message);
    }

    /**
     * A pre-formatted log prefix for this plugin.
     */
    private static final String PREFIX = String.format("%s[%s%sAdminHunt%s%s] %s[%sLOG%s] %s",
            ChatColor.GRAY, ChatColor.RED, ChatColor.BOLD, ChatColor.RESET, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.GOLD, ChatColor.DARK_GRAY, ChatColor.GRAY);

}
