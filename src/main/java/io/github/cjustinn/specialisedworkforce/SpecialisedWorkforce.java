package io.github.cjustinn.specialisedworkforce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import net.coreprotect.CoreProtectAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.transform.Result;

public final class SpecialisedWorkforce extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    
    private static Economy econ = null;
    private FileConfiguration config = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        // Create / access config file.
        config = this.getConfig();

        createConfigIfMissing();

        // Parse the config file values into the proper places.
        WorkforceManager.MaximumJobLevel = config.getInt("maxLevel", 50);
        WorkforceManager.experienceGainEquation = config.getString("experienceEquation");
        WorkforceManager.experienceRequirementEquation = config.getString("experienceRequirementEquation");
        WorkforceManager.maxPlayerJobs = config.getInt("maxJobs", 1);
        WorkforceManager.QuitJobDecreaseRate = config.getDouble("quitJobDecreaseRate", 0.2);

        SQLManager.host = config.getString("mysql.host");
        SQLManager.port = config.getString("mysql.port");
        SQLManager.database = config.getString("mysql.database");
        SQLManager.user = config.getString("mysql.user");
        SQLManager.pass = config.getString("mysql.pass");
        
        Set<String> jobKeys = config.getConfigurationSection("jobs").getKeys(false);
        for (String key : jobKeys) {
            String jobName = config.getString("jobs." + key + ".name");
            List<String> jobDesc = config.getStringList("jobs." + key + ".description");
            boolean  jobPaymentsEnabled = config.getBoolean("jobs." + key + ".payment.enabled");
            String jobPaymentEquation = config.getString("jobs." + key + ".payment.equation");
            Material jobIconMaterial = Material.getMaterial(config.getString("jobs." + key + ".icon").toUpperCase());

            if (jobIconMaterial == null)
                jobIconMaterial = Material.PLAYER_HEAD;

            List<JobAttribute> jobAttributes = new ArrayList<JobAttribute>();

            Set<String> attrKeys = config.getConfigurationSection("jobs." + key + ".attributes").getKeys(false);
            for (String attr : attrKeys) {
                    List<String> attrTargets = config.getStringList("jobs." + key + ".attributes." + attr + ".targets");
                    String attrEquation = config.getString("jobs." + key + ".attributes." + attr + ".equation");
                    String attrChance = config.getString("jobs." + key + ".attributes." + attr + ".chance");
                    JobAttributeType attrType = JobAttributeType.valueOf(attr);

                    jobAttributes.add(new JobAttribute(attrType, attrTargets, attrEquation, attrChance));
            }

            Job job = new Job(jobName, jobDesc, jobPaymentsEnabled, jobPaymentEquation, jobAttributes, jobIconMaterial);
            WorkforceManager.jobs.add(job);
        }

        log.info(String.format("[%s] Loaded %d jobs.", getDescription().getName(), WorkforceManager.jobs.size()));
        
        // Init. economy connection via Vault.
        if (initialiseEconomy()) {
            log.info(String.format("[%s] Interfaced with Vault successfully. Start-up continuing...", getDescription().getName()));

            // Connect to the DB.
            if (SQLManager.Connect()) {
                log.info(String.format("[%s] MySQL DB connection successful.", getDescription().getName()));

                // Create the db & table if it doesn't exist yet.
                try {
                    log.info(String.format("[%s] Initialising MySql tables...", getDescription().getName()));
                    PreparedStatement createTableStatement = SQLManager.GetConnection().prepareStatement("CREATE TABLE IF NOT EXISTS JobData (uuid varchar(250) not null, job varchar(250) not null, level int default 1 not null, experience int default 0 not null, started datetime default current_timestamp not null, primary key (uuid, job));");
                    createTableStatement.executeUpdate();

                    createTableStatement.close();

                    PreparedStatement createTableTwoStatement = SQLManager.GetConnection().prepareStatement("CREATE TABLE IF NOT EXISTS UserData (uuid varchar(250) not null, job varchar(250) not null, primary key (uuid, job));");
                    createTableTwoStatement.executeUpdate();

                    createTableTwoStatement.close();
                } catch(SQLException e) {
                    log.severe(String.format("[%s] Could not create the necessary MySQL tables.", getDescription().getName()));
                }

                // Fetch all player data from the table.
                log.info(String.format("[%s] Fetching user data...", getDescription().getName()));

                try {
                    PreparedStatement getUsersStatement = SQLManager.GetConnection().prepareStatement("SELECT UserData.uuid, UserData.job, JobData.level, JobData.experience FROM UserData INNER JOIN JobData ON UserData.uuid = JobData.uuid AND UserData.job = JobData.job;");
                    ResultSet userDataResults = getUsersStatement.executeQuery();

                    while (userDataResults.next()) {
                        String uuid = userDataResults.getString(1);
                        String jobName = userDataResults.getString(2);
                        int level = userDataResults.getInt(3);
                        int experience = userDataResults.getInt(4);

                        if (WorkforceManager.PlayerExists(uuid)) {
                            WorkforceManager.AddJobToPlayer(uuid, new PlayerJob(uuid, WorkforceManager.GetJobByName(jobName), level, experience));
                        } else {
                            List<PlayerJob> plyJobs = new ArrayList<PlayerJob>();
                            plyJobs.add(new PlayerJob(uuid, WorkforceManager.GetJobByName(jobName), level, experience));

                            WorkforceManager.playerData.add(new PlayerJobData(uuid, plyJobs));
                        }
                    }

                    getUsersStatement.close();
                    userDataResults.close();

                    log.info(String.format("[%s] User data has been fetched successfully. Loaded %d users.", getDescription().getName(), WorkforceManager.playerData.size()));
                } catch (SQLException e) {
                    log.severe(String.format("[%s] Could not fetch user data.", getDescription().getName()));
                }

                // REGISTER COMMANDS & LISTENERS
                this.getCommand("workforce").setExecutor(new CommandWorkforce());
                this.getCommand("workforce").setTabCompleter(new CommandWorkforceTabCompletion());
                this.getCommand("workforceadmin").setExecutor(new CommandWorkforceAdmin());
                this.getCommand("workforceadmin").setTabCompleter(new CommandWorkforceAdminTabCompletion());

                getServer().getPluginManager().registerEvents(new WorkforceListener(), this);
                getServer().getPluginManager().registerEvents(new CustomInventoryListener(), this);

                log.info(String.format("[%s] Loading has been completed successfully.", getDescription().getName()));
            } else {
                log.severe(String.format("[%s] Failed to initialise the MySQL connection. This plugin is now disabled.", getDescription().getName()));
                getServer().getPluginManager().disablePlugin(this);
            }
        } else {
            log.severe(String.format("[%s] Vault is not installed! This plugin is now disabled.", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (SQLManager.Disconnect()) {
            log.info(String.format("[%s] See you next time!", getDescription().getName()));
        } else {
            log.severe(String.format("[%s] Failed to disconnect from the MySQL database!", getDescription().getName()));
        }
    }

    public static boolean PayPlayer(String uuid, double amount) {
        EconomyResponse resp = econ.depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), amount);

        return resp.transactionSuccess();
    }

    private void createConfigIfMissing() {
        // MySQL configuration defaults.
        config.addDefault("mysql.enabled", true);
        config.addDefault("mysql.host", "localhost");
        config.addDefault("mysql.port", "3306");
        config.addDefault("mysql.user", "");
        config.addDefault("mysql.pass", "");
        config.addDefault("mysql.database", "");

        // Leveling defaults
        config.addDefault("maxLevel", 50);
        config.addDefault("experienceEquation", "5 * (1 + ({level} * 0.5))");
        config.addDefault("experienceRequirementEquation", "100 * (1 + ({level} * 5))");

        // Job defaults
        config.addDefault("maxJobs", 1);
        config.addDefault("quitJobDecreaseRate", 0.20);

        // Example job config default
        config.addDefault("jobs.farmer.name", "Farmer");
        config.addDefault("jobs.farmer.description", new String[] { "Receives bonuses to crop yields", "and a chance to negate damage to", "their hoes." });
        config.addDefault("jobs.farmer.icon", "DIAMOND_HOE");
        config.addDefault("jobs.farmer.payment.enabled", true);
        config.addDefault("jobs.farmer.payment.equation", "0.25 * (1 + ({level} * 0.38))");
        config.addDefault("jobs.farmer.attributes.BONUS_BLOCK_DROPS.targets", new String[] { "CARROT", "POTATO", "BEETROOT", "MELON", "PUMPKIN", "WHEAT" });
        config.addDefault("jobs.farmer.attributes.BONUS_BLOCK_DROPS.equation", "1 * (1 + ({level} * 0.06))");
        config.addDefault("jobs.farmer.attributes.BONUS_BLOCK_DROPS.chance", "0.1 + ({level} * 0.014)");
        config.addDefault("jobs.farmer.attributes.DURABILITY_SAVE.targets", new String[] { "{*}_HOE" });
        config.addDefault("jobs.farmer.attributes.DURABILITY_SAVE.chance", "0.1 + ({level} * 0.013)");

        config.options().copyDefaults(true);
        saveConfig();

        config = this.getConfig();
    }

    private boolean initialiseEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        econ = rsp.getProvider();
        return true;
    }
}
