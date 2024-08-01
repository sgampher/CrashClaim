package net.crashcraft.crashclaim.config;

import net.crashcraft.crashclaim.visualize.api.VisualColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class GlobalConfig extends BaseConfig{
    public static String locale;
    public static String paymentProvider;
    public static boolean skipNaturalMobGrief;

    private static void loadGeneral(){
        locale = getString("language", "en_US");
        paymentProvider = getString("payment-provider", "default");
        skipNaturalMobGrief = getBoolean("skip-natural-mob-grief", false);
    }

    public static HashMap<String, GroupSettings> groupSettings;

    private static void loadGroups() {
        groupSettings = new HashMap<>();

        final String baseKey = "group-settings";
        ConfigurationSection section = config.getConfigurationSection(baseKey);
        Set<String> keys;
        if (section == null) {
            keys = new HashSet<>();
        } else {
            keys = section.getKeys(false);
        }

        keys.add("default"); // Always need a default group.

        for (String groupName : keys) {
            groupSettings.put(groupName, new GroupSettings(
                    getInt(baseKey + "." + groupName + ".max-claims", -1)
            ));
        }
    }

    public static String visual_type;
    public static boolean visual_use_highest_block;
    public static HashMap<UUID, Material> visual_menu_items;

    private static void loadVisual(){
        visual_type = getString("visualization.visual-type", "glow");

        visual_menu_items = new HashMap<>();

        for (World world : Bukkit.getWorlds()){
            visual_menu_items.put(world.getUID(), Material.getMaterial(getString("visualization.claim-items." + world.getName(), Material.OAK_FENCE.name())));
        }

        visual_use_highest_block = getBoolean("visualization.visual-use-highest-block", false);

        setVisualBlockColor(VisualColor.GOLD, Material.ORANGE_CONCRETE);
        setVisualBlockColor(VisualColor.RED, Material.RED_CONCRETE);
        setVisualBlockColor(VisualColor.GREEN, Material.LIME_CONCRETE);
        setVisualBlockColor(VisualColor.YELLOW, Material.GREEN_CONCRETE);
        setVisualBlockColor(VisualColor.WHITE, Material.WHITE_CONCRETE);
    }

    private static void setVisualBlockColor(VisualColor color, Material defaultMaterial){
        String key = "visualization.visual-colors." + color.name();
        Material material = Material.getMaterial(getString(key, defaultMaterial.name()));

        if (material != null) {
            color.setMaterial(material);
            return;
        }

        log("Invalid material for " + key + ", loading default value");
        color.setMaterial(defaultMaterial);
    }

    public static boolean useCommandInsteadOfEdgeEject;
    public static String claimEjectCommand;
    public static boolean allowPlayerClaimTeleporting;

    private static void loadEject(){
        useCommandInsteadOfEdgeEject = getBoolean("eject.useCommandInstead", false);
        claimEjectCommand = getString("eject.command", "home");
        allowPlayerClaimTeleporting = getBoolean("allow-player-teleport-claim", false);
    }

    public static HashMap<PlayerTeleportEvent.TeleportCause, Integer> teleportCause;

    private static void loadTeleport(){
        // 0 | NONE  - diable, 1 | BLOCK - enable check with blocking, 2 | RELOCATE - enable check with relocating
        teleportCause = new HashMap<>();
        for (PlayerTeleportEvent.TeleportCause cause : PlayerTeleportEvent.TeleportCause.values()){
            if (cause.equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)){
                continue;
            }

            String value = getString("events.teleport." + cause.name(), "block");

            switch (value.toLowerCase()){
                case "none":
                    teleportCause.put(cause, 0);
                case "block":
                    teleportCause.put(cause, 1);
                case "relocate":
                    teleportCause.put(cause, 2);
                default:
                    //Bad value default to good one
                    teleportCause.put(cause, 1);
            }
        }
    }

    // public static double money_per_block;
    private static SortedMap<String, Double> money_per_block_group;
    public static ArrayList<UUID> disabled_worlds;
    public static String forcedVersionString;
    public static boolean blockPvPInsideClaims;
    public static boolean checkEntryExitWhileFlying;

    public static double numberOfDaysTillExpiration;
    public static String expiredMessage;

    public static int maxClaimBlocks;

    public static double getCostOfBlock(UUID player) {
        return getCostOfBlock(Objects.requireNonNull(Bukkit.getPlayer(player)));
    }

    public static double getCostOfBlock(Player player) {
        if (player.hasPermission("crashclaim.free")) {
            return 0;
        }

        LuckPerms luckPermsProvider = LuckPermsProvider.get();

        // Find which group matches sorted map key (group name)
        ArrayList<String> keySet = new ArrayList<>(money_per_block_group.keySet());
        Collections.reverse(keySet);
        User user = luckPermsProvider.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return money_per_block_group.get("default");
        }
        Collection<Group> groups = user.getInheritedGroups(user.getQueryOptions());
        // Return the group with the highest weight that's in keySet
        return money_per_block_group.getOrDefault(groups.stream().filter(group -> keySet.contains(group.getName())).reduce(
                (acc, val) -> acc.getWeight().getAsInt() > val.getWeight().getAsInt() ? acc : val
        ).get().getName(), money_per_block_group.get("default"));
    }

    private static void miscValues(){
        // money_per_block = getDouble("money-per-block", 0.01);
        money_per_block_group = new TreeMap<>();
        ConfigurationSection section = config.getConfigurationSection("group-cost");
        if (section != null){
            for (String key : section.getKeys(false)){
                money_per_block_group.put(key, section.getDouble(key + ".money-per-block"));
            }
        }
        else {
            money_per_block_group.put("default", 0.5);
        }
        disabled_worlds = new ArrayList<>();
        for (String s : getStringList("disabled-worlds", Collections.emptyList())){
            World world = Bukkit.getWorld(s);
            if (world == null){
                logError("World name was invalid or the world was not loaded into memory");
                continue;
            }

            disabled_worlds.add(world.getUID());
        }

        forcedVersionString = config.getString("use-this-version-instead");
        blockPvPInsideClaims = getBoolean("block-pvp-inside-claims", false);
        checkEntryExitWhileFlying = config.getBoolean("check-entry-and-exit-while-flying", false);

        numberOfDaysTillExpiration = getDouble("expired-claims.number-of-days", 30);
        expiredMessage = getString("expired-claims.message", "Your claim has expired due to inactivity at <COORDS> [Click to Teleport]");
        maxClaimBlocks = getInt("max-claim-blocks", -1);
    }

    public static boolean bypassModeBypassesMoney;

    private static void onBypass(){
        bypassModeBypassesMoney = getBoolean("bypass-mode-bypasses-payment", false);
    }


    public static boolean useStatistics;

    private static void onStats(){
        useStatistics = getBoolean("statistics", true);
    }
}
