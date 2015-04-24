package com.koletar.jj.chestkeeper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author jjkoletar
 * Modified by mattgd
 */
public class ChestKeeper extends JavaPlugin {
    public static Logger logger;
    private static final boolean TRACE = false;
    private Map<String, CKUser> users;
    private List<String> fileUsers;
    private final Map<String, String> ioQueue = new HashMap<String, String>();
    private CKFacilitator facilitator;
    private ThreadIO io;
    private YamlConfiguration serializer;
    private Economy economy;
    private boolean needsUpdate = false;
    private boolean updateIsCritical = false;

    public static final class Config {
        private static int maxNumberOfChests = 10;
        private static double chestOpenPrice = 0;
        private static double normalChestPrice = 1000;
        private static double largeChestPrice = 2000;
        private static int wandItemId = 0;
        private static List<String> disabledWorlds = new LinkedList<String>();

        public static int getMaxNumberOfChests() {
            return maxNumberOfChests;
        }

        private static void setMaxNumberOfChests(int maxNumberOfChests) {
            Config.maxNumberOfChests = maxNumberOfChests;
        }

        public static void writeMaxNumberOfChests(BufferedWriter out) throws IOException {
            out.write("# Maximum number of chests a user may own. Users with the chestkeeper.override permission are not bound by this value. -1 = no limit");
            out.newLine();
            out.write("maxNumberOfChests: 10");
            out.newLine();
        }

        public static double getChestOpenPrice() {
            return chestOpenPrice;
        }

        private static void setChestOpenPrice(double chestOpenPrice) {
            Config.chestOpenPrice = chestOpenPrice;
        }

        public static void writeChestOpenPrice(BufferedWriter out) throws IOException {
            out.write("# Economy price to open a chest. 0 disables charging to open a chest.");
            out.newLine();
            out.write("chestOpenPrice: 0");
            out.newLine();
        }

        public static double getNormalChestPrice() {
            return normalChestPrice;
        }

        private static void setNormalChestPrice(double normalChestPrice) {
            Config.normalChestPrice = normalChestPrice;
        }

        public static void writeNormalChestPrice(BufferedWriter out) throws IOException {
            out.write("# Price of a normal chest. 0 makes normal chests free.");
            out.newLine();
            out.write("normalChestPrice: 1000");
            out.newLine();
        }

        public static double getLargeChestPrice() {
            return largeChestPrice;
        }

        private static void setLargeChestPrice(double largeChestPrice) {
            Config.largeChestPrice = largeChestPrice;
        }

        public static void writeLargeChestPrice(BufferedWriter out) throws IOException {
            out.write("# Price of a large (double) chest. 0 makes large chests free. ");
            out.newLine();
            out.write("# The price to upgrade a large chest is largeChestPrice - normalChestPrice. If the two are equal, upgrades are free.");
            out.newLine();
            out.write("largeChestPrice: 2000");
            out.newLine();
        }

        public static int getWandItemId() {
            return wandItemId;
        }

        private static void setWandItemId(int wandItemId) {
            Config.wandItemId = wandItemId;
        }

        public static void writeWandItemId(BufferedWriter out) throws IOException {
            out.write("# Item ID of a 'wand' item which, when used while the item is in hand, opens a user's default chest, set to 0 to disable feature.");
            out.newLine();
            out.write("# Users will need the 'chestkeeper.use.wand' permission. Check http://www.minecraftwiki.net/wiki/Data_values for item IDs.");
            out.newLine();
            out.write("wandItemId: 0");
            out.newLine();
        }

        public static List<String> getDisabledWorlds() {
            return new LinkedList<String>(disabledWorlds);
        }

        public static boolean isWorldDisabled(String worldName) {
            return disabledWorlds.contains(worldName);
        }

        public static void setDisabledWorlds(List<String> disabledWorlds) {
            Config.disabledWorlds = disabledWorlds;
        }

        public static void writeDisabledWorlds(BufferedWriter out) throws IOException {
            out.write("# A list of worlds where ChestKeeper Chests cannot be opened in. All other actions are allowed.");
            out.newLine();
            out.write("# World names are case-sensitive. Users with the chestkeeper.override permission are allowed to use Chests in these worlds.");
            out.newLine();
            out.write("disabledWorlds: ");
            out.newLine();
            out.write("  - myleastfavoriteworld");
            out.newLine();
        }
    }

    public static final class YMLFilter implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.endsWith(".yml");
        }
    }

    public static final class ChestYMLFilter implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.endsWith(".chestYml");
        }
    }

    protected static String getFileName(String uuid) {
        return uuid.toLowerCase() + ".yml";
    }

    static {
        ConfigurationSerialization.registerClass(CKUser.class);
        ConfigurationSerialization.registerClass(CKChest.class);
    }

    public void onEnable() {
        logger = getLogger();
        logger.info("Enabling ChestKeeper " + getDescription().getVersion() + ".");
        Phrases.getInstance().initialize(Locale.ENGLISH);
        users = new HashMap<String, CKUser>();
        fileUsers = new LinkedList<String>();
        try {
            loadConfiguration();
        } catch (IOException e) {
            logger.severe("Unable to load configuration!");
            e.printStackTrace();
        }
        loadData();
        facilitator = new CKFacilitator(this);
        serializer = new YamlConfiguration();
        io = new ThreadIO(ioQueue, new File(getDataFolder(), "data"));
        getServer().getScheduler().runTaskAsynchronously(this, io);
        getCommand("chestkeeper").setExecutor(facilitator);
        getServer().getPluginManager().registerEvents(facilitator, this);
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        } else {
            logger.severe("ChestKeeper was unable to initialize Economy Interface with Vault! Prices are now 0!");
            Config.setChestOpenPrice(0);
            Config.setNormalChestPrice(0);
            Config.setLargeChestPrice(0);
        }
        logger.info("ChestKeeper " + getDescription().getVersion() + " enabled!");
    }

    public void onDisable() {
        logger.info("Disabling...");

        for (CKUser user : users.values()) {
            user.forceClean();
        }
        io.shutdown();
        YamlConfiguration conf = new YamlConfiguration();
        for (CKUser user : users.values()) {
            File out = new File(new File(getDataFolder(), "data"), ChestKeeper.getFileName(user.getUUID().toString()));
            conf.set("user", user);
            try {
                conf.save(out);
            } catch (IOException e) {
                logger.warning("ChestKeeper couldn't save a player during shutdown!");
            }
        }

        logger.info("ChestKeeper disabled!");
    }

    public static void trace(String message) {
        if (TRACE) {
            logger.info("[Trace] " + message);
        }
    }

    private void loadConfiguration() throws IOException {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
            out.write("# ChestKeeper configuration file");
            out.newLine();
            Config.writeMaxNumberOfChests(out);
            Config.writeChestOpenPrice(out);
            Config.writeNormalChestPrice(out);
            Config.writeLargeChestPrice(out);
            Config.writeWandItemId(out);
            Config.writeDisabledWorlds(out);
            out.close();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        BufferedWriter out = new BufferedWriter(new FileWriter(configFile, true));
        if (config.contains("maxNumberOfChests")) {
            Config.setMaxNumberOfChests(config.getInt("maxNumberOfChests"));
        } else {
            Config.writeMaxNumberOfChests(out);
        }
        if (config.contains("chestOpenPrice")) {
            Config.setChestOpenPrice(config.getDouble("chestOpenPrice"));
        } else {
            Config.writeChestOpenPrice(out);
        }
        if (config.contains("normalChestPrice")) {
            Config.setNormalChestPrice(config.getDouble("normalChestPrice"));
        } else {
            Config.writeNormalChestPrice(out);
        }
        if (config.contains("largeChestPrice")) {
            Config.setLargeChestPrice(config.getDouble("largeChestPrice"));
        } else {
            Config.writeLargeChestPrice(out);
        }
        if (config.contains("wandItemId")) {
            Config.setWandItemId(config.getInt("wandItemId"));
        } else {
            Config.writeWandItemId(out);
        }
        if (config.contains("disabledWorlds")) {
            Config.setDisabledWorlds(config.getStringList("disabledWorlds"));
        } else {
            Config.writeDisabledWorlds(out);
        }
        out.close();
    }

    private void loadData() {
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        for (File file : dataFolder.listFiles(new YMLFilter())) {
            trace(file.getName());
            fileUsers.add(file.getName().replace(".yml", ""));
        }
    }

    public boolean needsUpdate() {
        return needsUpdate;
    }

    public boolean isUpdateCritical() {
        return updateIsCritical;
    }

    protected Economy getEconomy() {
        return economy;
    }

    protected boolean hasEconomy() {
        return economy != null;
    }

    private void loadUser(String username, String uuid) {
        if (!users.containsKey(username) && fileUsers.contains(uuid)) {
            trace("loading " + uuid);
            File userFile = new File(new File(getDataFolder(), "data"), getFileName(uuid));
            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
            if (userConfig.contains("user")) {
                Object o = userConfig.get("user");
                if (o instanceof CKUser) {
                    users.put(username, (CKUser) o);
                    fileUsers.remove(uuid);
                } else {
                    logger.severe("Error loading user " + uuid + ", wasn't a CKUser object.");
                }
            }
        }
    }

    /**
     * Get a user by a full username, and create them if they don't exist.
     *
     * @param username Username to get
     * @return CKUser of the username
     */
    protected CKUser getUser(String username) {
    	// Get the user's UUID
    	Player chestPlayer = Bukkit.getPlayer(username);
    	String uuid;
    	if (chestPlayer == null) {
    		UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(username));
			Map<String, UUID> response = null;
			try {
				response = fetcher.call();
			} catch (Exception e) {
				logger.severe("Exception while running UUIDFetcher");
				e.printStackTrace();
			}
			uuid = response.get(username).toString();
    	} else {
    		uuid = chestPlayer.getUniqueId().toString();
    	}
        if (fileUsers.contains(uuid)) {
            loadUser(username, uuid);
        }
        if (users.containsKey(username)) {
            return users.get(username);
        } else {
            CKUser user = new CKUser(username);
            users.put(username, user);
            return user;
        }
    }

    /**
     * Get a user from a player object, and create them if they don't exist.
     *
     * @param p Bukkit player
     * @return CKUser
     */
    protected CKUser getUser(Player p) {
        return getUser(p.getName());
    }

    /**
     * Attempt to match a user by either a full or partial username.
     *
     * @param username Username to match
     * @return null if no user was found, else the CKUser
     */
    protected CKUser matchUser(String username) {
        String matchedUser = null;
        
        // Get the user's UUID
        Player chestPlayer = Bukkit.getPlayer(username);
    	String uuid;
    	if (chestPlayer == null) {
    		UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(username));
			Map<String, UUID> response = null;
			try {
				response = fetcher.call();
			} catch (Exception e) {
				logger.severe("Exception while running UUIDFetcher");
				e.printStackTrace();
			}
			uuid = response.get(username).toString();
    	} else {
    		uuid = chestPlayer.getUniqueId().toString();
    	}
        for (String fileUser : fileUsers) {
            boolean matched = fileUser.contains(uuid);
            if (matchedUser == null && matched) {
                matchedUser = username;
            } else if (matchedUser != null && matched) {
                // Ended up matching two users
                return null;
            }
        }
        for (String memoryUser : users.keySet()) {
            boolean matched = memoryUser.equalsIgnoreCase(username);
            if (matchedUser == null && matched) {
                matchedUser = memoryUser;
            } else if (matchedUser != null && matched) {
                return null;
            }
        }
        return matchedUser == null ? null : getUser(matchedUser);
    }

    /**
     * Add a modified CKUser to the io queue
     *
     * @param user CKUser to save
     */
    protected void queueUser(CKUser user) {
        trace("queuing user " + user.getUUID());
        final String uuid = user.getUUID().toString();
        serializer.set("user", user);
        final String serialized = serializer.saveToString();
        synchronized (ioQueue) {
            ioQueue.put(uuid, serialized);
            ioQueue.notifyAll();
        }
    }
}
