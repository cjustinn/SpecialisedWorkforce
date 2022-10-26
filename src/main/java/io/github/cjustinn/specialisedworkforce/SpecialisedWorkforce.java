package io.github.cjustinn.specialisedworkforce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpecialisedWorkforce extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private FileConfiguration config = null;

    public SpecialisedWorkforce() {
    }

    public void onEnable() {
        this.config = this.getConfig();
        this.createConfigIfMissing();
        WorkforceManager.MaximumJobLevel = this.config.getInt("maxLevel", 50);
        WorkforceManager.experienceGainEquation = this.config.getString("experienceEquation");
        WorkforceManager.experienceRequirementEquation = this.config.getString("experienceRequirementEquation");
        WorkforceManager.maxPlayerJobs = this.config.getInt("maxJobs", 1);
        WorkforceManager.QuitJobDecreaseRate = this.config.getDouble("quitJobDecreaseRate", 0.2);
        SQLManager.TrackPlayerGear = this.config.getBoolean("trackGear", true);
        SQLManager.host = this.config.getString("mysql.host");
        SQLManager.port = this.config.getString("mysql.port");
        SQLManager.database = this.config.getString("mysql.database");
        SQLManager.user = this.config.getString("mysql.user");
        SQLManager.pass = this.config.getString("mysql.pass");
        Set<String> jobKeys = this.config.getConfigurationSection("jobs").getKeys(false);
        Iterator var2 = jobKeys.iterator();

        String uuid;
        while(var2.hasNext()) {
            String key = (String)var2.next();
            uuid = this.config.getString("jobs." + key + ".name");
            List<String> jobDesc = this.config.getStringList("jobs." + key + ".description");
            boolean jobPaymentsEnabled = this.config.getBoolean("jobs." + key + ".payment.enabled");
            String jobPaymentEquation = this.config.getString("jobs." + key + ".payment.equation");
            Material jobIconMaterial = Material.getMaterial(this.config.getString("jobs." + key + ".icon").toUpperCase());
            if (jobIconMaterial == null) {
                jobIconMaterial = Material.PLAYER_HEAD;
            }

            List<JobAttribute> jobAttributes = new ArrayList();
            Set<String> attrKeys = this.config.getConfigurationSection("jobs." + key + ".attributes").getKeys(false);
            Iterator var11 = attrKeys.iterator();

            while(var11.hasNext()) {
                String attr = (String)var11.next();
                List<String> attrTargets = this.config.getStringList("jobs." + key + ".attributes." + attr + ".targets");
                String attrEquation = this.config.getString("jobs." + key + ".attributes." + attr + ".equation");
                String attrChance = this.config.getString("jobs." + key + ".attributes." + attr + ".chance");
                JobAttributeType attrType = JobAttributeType.valueOf(attr);
                jobAttributes.add(new JobAttribute(attrType, attrTargets, attrEquation, attrChance));
            }

            Job job = new Job(uuid, jobDesc, jobPaymentsEnabled, jobPaymentEquation, jobAttributes, jobIconMaterial);
            WorkforceManager.jobs.add(job);
        }

        log.info(String.format("[%s] Loaded %d jobs.", this.getDescription().getName(), WorkforceManager.jobs.size()));
        if (this.initialiseEconomy()) {
            log.info(String.format("[%s] Interfaced with Vault successfully. Start-up continuing...", this.getDescription().getName()));
            if (SQLManager.Connect()) {
                log.info(String.format("[%s] MySQL DB connection successful.", this.getDescription().getName()));

                PreparedStatement getUsersStatement;
                try {
                    log.info(String.format("[%s] Initialising MySql tables...", this.getDescription().getName()));
                    getUsersStatement = SQLManager.GetConnection().prepareStatement("CREATE TABLE IF NOT EXISTS JobData (uuid varchar(250) not null, job varchar(250) not null, level int default 1 not null, experience int default 0 not null, started datetime default current_timestamp not null, primary key (uuid, job));");
                    getUsersStatement.executeUpdate();
                    getUsersStatement.close();
                    PreparedStatement createTableTwoStatement = SQLManager.GetConnection().prepareStatement("CREATE TABLE IF NOT EXISTS UserData (uuid varchar(250) not null, job varchar(250) not null, primary key (uuid, job));");
                    createTableTwoStatement.executeUpdate();
                    createTableTwoStatement.close();
                    if (SQLManager.TrackPlayerGear) {
                        PreparedStatement createPlayerGear = SQLManager.GetConnection().prepareStatement("CREATE TABLE IF NOT EXISTS PlayerGear (id int not null auto_increment, item_owner varchar(250) not null, item_name varchar(500) not null, base_gear_score int not null default 0, item_type varchar(250) not null, primary key (id));");
                        createPlayerGear.executeUpdate();
                        createPlayerGear.close();
                        PreparedStatement createGearEnchantments = SQLManager.GetConnection().prepareStatement("CREATE TABLE IF NOT EXISTS GearEnchantment (item_id int not null, enchantment_name varchar(250) not null, gear_score int not null default 0, primary key (item_id, enchantment_name), foreign key (item_id) references PlayerGear(id));");
                        createGearEnchantments.executeUpdate();
                        createGearEnchantments.close();
                    }
                } catch (SQLException var17) {
                    log.severe(String.format("[%s] Could not create the necessary MySQL tables.", this.getDescription().getName()));
                }

                log.info(String.format("[%s] Fetching user data...", this.getDescription().getName()));

                try {
                    getUsersStatement = SQLManager.GetConnection().prepareStatement("SELECT UserData.uuid, UserData.job, JobData.level, JobData.experience FROM UserData INNER JOIN JobData ON UserData.uuid = JobData.uuid AND UserData.job = JobData.job;");
                    ResultSet userDataResults = getUsersStatement.executeQuery();

                    while(userDataResults.next()) {
                        uuid = userDataResults.getString(1);
                        String jobName = userDataResults.getString(2);
                        int level = userDataResults.getInt(3);
                        int experience = userDataResults.getInt(4);
                        if (WorkforceManager.PlayerExists(uuid)) {
                            WorkforceManager.AddJobToPlayer(uuid, new PlayerJob(uuid, WorkforceManager.GetJobByName(jobName), level, experience));
                        } else {
                            List<PlayerJob> plyJobs = new ArrayList();
                            plyJobs.add(new PlayerJob(uuid, WorkforceManager.GetJobByName(jobName), level, experience));
                            WorkforceManager.playerData.add(new PlayerJobData(uuid, plyJobs));
                        }
                    }

                    getUsersStatement.close();
                    userDataResults.close();
                    log.info(String.format("[%s] User data has been fetched successfully. Loaded %d users.", this.getDescription().getName(), WorkforceManager.playerData.size()));
                } catch (SQLException var18) {
                    log.severe(String.format("[%s] Could not fetch user data.", this.getDescription().getName()));
                }

                this.getCommand("workforce").setExecutor(new CommandWorkforce());
                this.getCommand("workforce").setTabCompleter(new CommandWorkforceTabCompletion());
                this.getCommand("workforceadmin").setExecutor(new CommandWorkforceAdmin());
                this.getCommand("workforceadmin").setTabCompleter(new CommandWorkforceAdminTabCompletion());
                this.getServer().getPluginManager().registerEvents(new WorkforceListener(), this);
                this.getServer().getPluginManager().registerEvents(new CustomInventoryListener(), this);
                log.info(String.format("[%s] Loading has been completed successfully.", this.getDescription().getName()));
            } else {
                log.severe(String.format("[%s] Failed to initialise the MySQL connection. This plugin is now disabled.", this.getDescription().getName()));
                this.getServer().getPluginManager().disablePlugin(this);
            }
        } else {
            log.severe(String.format("[%s] Vault is not installed! This plugin is now disabled.", this.getDescription().getName()));
            this.getServer().getPluginManager().disablePlugin(this);
        }

        SQLManager.Disconnect();
    }

    public void onDisable() {
        if (SQLManager.Disconnect()) {
            log.info(String.format("[%s] See you next time!", this.getDescription().getName()));
        } else {
            log.severe(String.format("[%s] Failed to disconnect from the MySQL database!", this.getDescription().getName()));
        }

    }

    public static boolean PayPlayer(String uuid, double amount) {
        EconomyResponse resp = econ.depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), amount);
        return resp.transactionSuccess();
    }

    private void createConfigIfMissing() {
        this.config.addDefault("mysql.enabled", true);
        this.config.addDefault("mysql.host", "localhost");
        this.config.addDefault("mysql.port", "3306");
        this.config.addDefault("mysql.user", "");
        this.config.addDefault("mysql.pass", "");
        this.config.addDefault("mysql.database", "");
        this.config.addDefault("maxLevel", 50);
        this.config.addDefault("experienceEquation", "5 * (1 + ({level} * 0.5))");
        this.config.addDefault("experienceRequirementEquation", "100 * (1 + ({level} * 5))");
        this.config.addDefault("maxJobs", 1);
        this.config.addDefault("quitJobDecreaseRate", 0.2);
        this.config.addDefault("jobs.farmer.name", "Farmer");
        this.config.addDefault("jobs.farmer.description", new String[]{"Receives bonuses to crop yields", "and a chance to negate damage to", "their hoes."});
        this.config.addDefault("jobs.farmer.icon", "DIAMOND_HOE");
        this.config.addDefault("jobs.farmer.payment.enabled", true);
        this.config.addDefault("jobs.farmer.payment.equation", "0.25 * (1 + ({level} * 0.38))");
        this.config.addDefault("jobs.farmer.attributes.BONUS_BLOCK_DROPS.targets", new String[]{"CARROT", "POTATO", "BEETROOT", "MELON", "PUMPKIN", "WHEAT"});
        this.config.addDefault("jobs.farmer.attributes.BONUS_BLOCK_DROPS.equation", "1 * (1 + ({level} * 0.06))");
        this.config.addDefault("jobs.farmer.attributes.BONUS_BLOCK_DROPS.chance", "0.1 + ({level} * 0.014)");
        this.config.addDefault("jobs.farmer.attributes.DURABILITY_SAVE.targets", new String[]{"{*}_HOE"});
        this.config.addDefault("jobs.farmer.attributes.DURABILITY_SAVE.chance", "0.1 + ({level} * 0.013)");
        this.config.options().copyDefaults(true);
        this.saveConfig();
        this.config = this.getConfig();
    }

    private boolean initialiseEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        } else {
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            } else {
                econ = (Economy)rsp.getProvider();
                return true;
            }
        }
    }
}
