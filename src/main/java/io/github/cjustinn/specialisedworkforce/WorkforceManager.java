package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;


import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngine;
import javax.script.ScriptException;



public class WorkforceManager {
    // Configurable variables
    public static int MaximumJobLevel = 0;
    public static int maxPlayerJobs = 0;
    public static double QuitJobDecreaseRate = 0.0;
    public static String experienceGainEquation = null;
    public static String experienceRequirementEquation = null;

    // Lists
    public static List<Job> jobs = new ArrayList<Job>();
    public static List<PlayerJobData> playerData = new ArrayList<PlayerJobData>();

    // Generic functions
    public static boolean PlayerHasJob(String uuid, String jobName) {
        if (PlayerIsEmployed(uuid)) {
            return GetPlayerData(uuid).playerHasJob(jobName);
        } else return false;
    }

    public static int GetRequiredExperienceAtLevel(int level) {
        int req = -1;

        ScriptEngineFactory sef = new NashornScriptEngineFactory();
        ScriptEngine sEngine = sef.getScriptEngine();

        String eq = experienceRequirementEquation.replace("{level}", String.format("%d", level));

        try {
            req = new BigDecimal(sEngine.eval(eq).toString()).intValue();
        } catch (ScriptException e) {
            e.printStackTrace();
        }

        return req;
    }

    public static boolean PlayerExists(String uuid) {
        return GetPlayerIndex(uuid) > -1;
    }

    public static void AddJobToPlayer(String uuid, PlayerJob job) {
        playerData.get(GetPlayerIndex(uuid)).addJobToPlayer(job);
    }

    // Player data functions.
    // Get the index of the desired player (by uuid) from the player data list.
    private static int GetPlayerIndex(String uuid) {
        int idx = -1;

        for (int i = 0; i < playerData.size() && idx < 0; i++) {
            if (playerData.get(i).getUuid().equalsIgnoreCase(uuid)) {
                idx = i;
            }
        }

        return idx;
    }

    public static boolean PlayerBelowMaxJobs(String uuid) {
        return GetPlayerData(uuid).getJobCount() < maxPlayerJobs;
    }

    // Assigns a job to the player with the provided uuid by adding a new PlayerJobData obj. to the list
    // and adding the data to the database.
    public static boolean AssignJobToPlayer(String uuid, String jobName) {
        if (!PlayerIsEmployed(uuid) || (PlayerIsEmployed(uuid) && PlayerBelowMaxJobs(uuid))) {
            Job job = GetJobByName(jobName);
            if (job != null) {

                List<PlayerJob> jobs = new ArrayList<PlayerJob>();

                // Check if player has job data already.
                PlayerJob plyJob = null;

                try {
                    PreparedStatement jobDataStatement = SQLManager.GetConnection().prepareStatement("SELECT level, experience FROM JobData WHERE uuid=? AND job=? LIMIT 1;");
                    jobDataStatement.setString(1, uuid);
                    jobDataStatement.setString(2, jobName);

                    ResultSet data = jobDataStatement.executeQuery();
                    if (data.next()) {
                        // Data does exist.
                        plyJob = new PlayerJob(uuid, job, data.getInt(1), data.getInt(2));
                    } else {
                        // No data was returned.
                        PreparedStatement addJobDataStatement = SQLManager.GetConnection().prepareStatement("INSERT INTO JobData(uuid, job) VALUES(?,?);");
                        addJobDataStatement.setString(1, uuid);
                        addJobDataStatement.setString(2, jobName);

                        addJobDataStatement.executeUpdate();
                        addJobDataStatement.close();

                        plyJob = new PlayerJob(uuid, job, 1, 0);
                    }

                    PreparedStatement addUserDataStatement = SQLManager.GetConnection().prepareStatement("INSERT INTO UserData VALUES(?,?);");
                    addUserDataStatement.setString(1, uuid);
                    addUserDataStatement.setString(2, jobName);

                    addUserDataStatement.executeUpdate();

                    addUserDataStatement.close();
                    jobDataStatement.close();
                    data.close();
                } catch(SQLException e) {
                    e.printStackTrace();
                }

                if (plyJob != null) {
                    jobs.add(plyJob);

                    if (PlayerIsEmployed(uuid))
                        playerData.get(GetPlayerIndex(uuid)).addJobToPlayer(plyJob);
                    else playerData.add(new PlayerJobData(uuid, jobs));

                    return true;
                } else return false;
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "Job not found.");
                return false;
            }
        } else return false;
    }

    // Check if the player with the matching uuid has data stored.
    public static boolean PlayerIsEmployed(String uuid) {
        return playerData.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(uuid));
    }

    // Get the job of the player with the matching uuid.
    public static Job GetPlayerJob(String uuid, String jobName) {
        if (PlayerIsEmployed(uuid)) {
            return GetPlayerData(uuid).getJobByName(jobName);
        } else return null;
    }

    public static boolean PlayerHasJobWithAttribute(String uuid, JobAttributeType attr) {
        if (PlayerIsEmployed(uuid)) {
            return GetPlayerData(uuid).playerHasJobWithAttribute(attr);
        } else return false;
    }

    public static PlayerJob GetPlayerJobByAttribute(String uuid, JobAttributeType attr) {
        if (PlayerIsEmployed(uuid)) {
            return GetPlayerData(uuid).getPlayerJobByAttribute(attr);
        } else return null;
    }

    // Remove the player from their job (and the db) based on the uuid provided.
    public static boolean RemovePlayerJob(String uuid, String jobName) {
        if (PlayerIsEmployed(uuid) && PlayerHasJob(uuid, jobName)) {
            // Remove the player from the database based on the uuid.
            try {
                // Remove the job from the UserData listing of active jobs.
                PreparedStatement removeStatement = SQLManager.GetConnection().prepareStatement("DELETE FROM UserData WHERE uuid=? AND job=?;");
                removeStatement.setString(1, uuid);
                removeStatement.setString(2, jobName);

                removeStatement.executeUpdate();
                removeStatement.close();

                // Decrease the player level by the JobQuitDecreaseRate and update the JobData listing.
                PlayerJob quitJob = playerData.get(GetPlayerIndex(uuid)).getPlayerJobByName(jobName);
                quitJob.setLevel((int) Math.floor(quitJob.getLevel() * (1 - QuitJobDecreaseRate)));
                quitJob.setExperiencePoints(0);

                PreparedStatement updateJobDataStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?;");
                updateJobDataStatement.setInt(1, quitJob.getLevel());
                updateJobDataStatement.setInt(2, quitJob.getExperience());
                updateJobDataStatement.setString(3, uuid);
                updateJobDataStatement.setString(4, jobName);

                updateJobDataStatement.executeUpdate();
                updateJobDataStatement.close();
            } catch(SQLException e) {
                return false;
            }

            // Remove the job from the player list.
            playerData.get(GetPlayerIndex(uuid)).removeJobByName(jobName);

            // Remove the player from the playerData list if they no longer have any jobs.
            if (playerData.get(GetPlayerIndex(uuid)).getJobCount() == 0) {
                playerData.removeIf(p -> p.getUuid().equals(uuid));
            }

            return true;

        } else return false;
    }

    // Increment the provided player's level.
    public static void AddLevelToPlayer(String uuid, String jobName, int amount) {
        if (PlayerIsEmployed(uuid)) {
            int idx = GetPlayerIndex(uuid);
            if (idx >= 0) {
                int jobIdx = GetPlayerData(uuid).getJobIndexByName(jobName);
                if (jobIdx >= 0) {
                    playerData.get(idx).addLevelToJobByIndex(jobIdx, amount);
                    PlayerJob _plyJob = playerData.get(idx).getJobByIndex(jobIdx);

                    // Update the player data in the database.
                    try {
                        PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?");
                        updateStatement.setInt(1, _plyJob.getLevel());
                        updateStatement.setInt(2, _plyJob.getExperience());
                        updateStatement.setString(3, uuid);
                        updateStatement.setString(4, jobName);

                        updateStatement.executeUpdate();
                        updateStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Set the provided player's experience.
    public static void SetPlayerExperience(String uuid, String jobName, int experience) {
        if (PlayerIsEmployed(uuid)) {
            int idx = GetPlayerIndex(uuid);
            if (idx >= 0) {
                int jobIdx = playerData.get(idx).getJobIndexByName(jobName);
                if (jobIdx >= 0) {
                    playerData.get(idx).setJobExperienceByIndex(jobIdx, experience);
                    PlayerJob _plyJob = playerData.get(idx).getJobByIndex(jobIdx);

                    // Update the player data in the database.
                    try {
                        PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?;");
                        updateStatement.setInt(1, _plyJob.getLevel());
                        updateStatement.setInt(2, _plyJob.getExperience());
                        updateStatement.setString(3, uuid);
                        updateStatement.setString(4, jobName);

                        updateStatement.executeUpdate();
                        updateStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Set the provided player's level.
    public static void SetPlayerLevel(String uuid, String job, int level) {
        if (PlayerIsEmployed(uuid)) {
            int idx = GetPlayerIndex(uuid);
            if (idx >= 0) {
                int jobIdx = playerData.get(idx).getJobIndexByName(job);

                if (jobIdx >= 0) {
                    playerData.get(idx).setJobLevelByIndex(jobIdx, level);
                    PlayerJob _plyJob = playerData.get(idx).getJobByIndex(jobIdx);

                    if (_plyJob == null) return;

                    // Update the player data in the database.
                    try {
                        PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?");
                        updateStatement.setInt(1, _plyJob.getLevel());
                        updateStatement.setInt(2, _plyJob.getExperience());
                        updateStatement.setString(3, uuid);

                        updateStatement.executeUpdate();
                        updateStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Add experience to the provided player.
    public static void AddExperiencePointsToPlayer(String uuid, String jobName, int amount) {
        if (PlayerIsEmployed(uuid)) {
            int idx = GetPlayerIndex(uuid);

            if (idx < 0) return;
            else {

                int jobIdx = playerData.get(idx).getJobIndexByName(jobName);
                if (jobIdx >= 0) {
                    // Update the player obj. in the list.
                    playerData.get(idx).addExperienceToJobByIndex(jobIdx, amount);
                    PlayerJob _plyJob = playerData.get(idx).getJobByIndex(jobIdx);

                    // Update the player data in the database.
                    try {
                        PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?;");
                        updateStatement.setInt(1, _plyJob.getLevel());
                        updateStatement.setInt(2, _plyJob.getExperience());
                        updateStatement.setString(3, uuid);
                        updateStatement.setString(4, jobName);

                        updateStatement.executeUpdate();
                        updateStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Return the player's job data.
    public static PlayerJobData GetPlayerData(String uuid) {
        if (PlayerIsEmployed(uuid)) {
            int idx = GetPlayerIndex(uuid);

            return idx < 0 ? null : playerData.get(idx);
        } else return null;
    }

    // Job Functions
    // Get a job's index in the list.
    private static int GetJobIndex(String name) {
        int idx = -1;

        for (int i = 0; i < jobs.size() && idx < 0; i++) {
            if (jobs.get(i).getName().toLowerCase().equals(name.toLowerCase())) {
                idx = i;
            }
        }

        return idx;
    }

    // Get a job by its name.
    public static Job GetJobByName(String name) {
        int idx = GetJobIndex(name);
        if (idx >= 0) {
            return jobs.get(idx);
        } else return null;
    }

    // Get a list of all players who have the provided job.
    public static List<PlayerJobData> GetPlayersByJob(String name) {
        return playerData.stream().filter(p -> p.playerHasJob(name) == true).collect(Collectors.toList());
    }

    // Check if a job exists.
    public static boolean JobExists(String name) {
        return jobs.stream().anyMatch(j -> j.getName().toLowerCase().equals(name.toLowerCase()));
    }

    public static Job GetJobBySelectionIcon(ItemStack target) {
        Job _j = null;

        for (Job job : jobs) {
            if (job.getSelectionIcon().equals(target))
                _j = job;
        }

        return _j;
    }
}
