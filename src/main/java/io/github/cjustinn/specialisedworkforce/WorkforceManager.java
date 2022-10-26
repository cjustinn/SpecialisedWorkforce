package io.github.cjustinn.specialisedworkforce;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class WorkforceManager {
    public static int MaximumJobLevel = 0;
    public static int maxPlayerJobs = 0;
    public static double QuitJobDecreaseRate = 0.0;
    public static String experienceGainEquation = null;
    public static String experienceRequirementEquation = null;
    public static List<Job> jobs = new ArrayList();
    public static List<PlayerJobData> playerData = new ArrayList();

    public WorkforceManager() {
    }

    public static boolean PlayerHasJob(String uuid, String jobName) {
        return PlayerIsEmployed(uuid) ? GetPlayerData(uuid).playerHasJob(jobName) : false;
    }

    public static int GetRequiredExperienceAtLevel(int level) {
        int req = -1;
        ScriptEngineFactory sef = new NashornScriptEngineFactory();
        ScriptEngine sEngine = sef.getScriptEngine();
        String eq = experienceRequirementEquation.replace("{level}", String.format("%d", level));

        try {
            req = (new BigDecimal(sEngine.eval(eq).toString())).intValue();
        } catch (ScriptException var6) {
            var6.printStackTrace();
        }

        return req;
    }

    public static boolean PlayerExists(String uuid) {
        return GetPlayerIndex(uuid) > -1;
    }

    public static void AddJobToPlayer(String uuid, PlayerJob job) {
        ((PlayerJobData)playerData.get(GetPlayerIndex(uuid))).addJobToPlayer(job);
    }

    private static int GetPlayerIndex(String uuid) {
        int idx = -1;

        for(int i = 0; i < playerData.size() && idx < 0; ++i) {
            if (((PlayerJobData)playerData.get(i)).getUuid().equalsIgnoreCase(uuid)) {
                idx = i;
            }
        }

        return idx;
    }

    public static boolean PlayerBelowMaxJobs(String uuid) {
        return GetPlayerData(uuid).getJobCount() < maxPlayerJobs;
    }

    public static boolean AssignJobToPlayer(String uuid, String jobName) {
        boolean success = false;
        if (SQLManager.Connect() && (!PlayerIsEmployed(uuid) || PlayerIsEmployed(uuid) && PlayerBelowMaxJobs(uuid))) {
            Job job = GetJobByName(jobName);
            if (job != null) {
                List<PlayerJob> jobs = new ArrayList();
                PlayerJob plyJob = null;

                try {
                    PreparedStatement jobDataStatement = SQLManager.GetConnection().prepareStatement("SELECT level, experience FROM JobData WHERE uuid=? AND job=? LIMIT 1;");
                    jobDataStatement.setString(1, uuid);
                    jobDataStatement.setString(2, jobName);
                    ResultSet data = jobDataStatement.executeQuery();
                    PreparedStatement addUserDataStatement;
                    if (data.next()) {
                        plyJob = new PlayerJob(uuid, job, data.getInt(1), data.getInt(2));
                    } else {
                        addUserDataStatement = SQLManager.GetConnection().prepareStatement("INSERT INTO JobData(uuid, job) VALUES(?,?);");
                        addUserDataStatement.setString(1, uuid);
                        addUserDataStatement.setString(2, jobName);
                        addUserDataStatement.executeUpdate();
                        addUserDataStatement.close();
                        plyJob = new PlayerJob(uuid, job, 1, 0);
                    }

                    addUserDataStatement = SQLManager.GetConnection().prepareStatement("INSERT INTO UserData VALUES(?,?);");
                    addUserDataStatement.setString(1, uuid);
                    addUserDataStatement.setString(2, jobName);
                    addUserDataStatement.executeUpdate();
                    addUserDataStatement.close();
                    jobDataStatement.close();
                    data.close();
                } catch (SQLException var9) {
                    var9.printStackTrace();
                }

                if (plyJob != null) {
                    jobs.add(plyJob);
                    if (PlayerIsEmployed(uuid)) {
                        ((PlayerJobData)playerData.get(GetPlayerIndex(uuid))).addJobToPlayer(plyJob);
                    } else {
                        playerData.add(new PlayerJobData(uuid, jobs));
                    }

                    success = true;
                }
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "Job not found.");
            }

            SQLManager.Disconnect();
        }

        return success;
    }

    public static boolean PlayerIsEmployed(String uuid) {
        return playerData.stream().anyMatch((p) -> {
            return p.getUuid().equalsIgnoreCase(uuid);
        });
    }

    public static Job GetPlayerJob(String uuid, String jobName) {
        return PlayerIsEmployed(uuid) ? GetPlayerData(uuid).getJobByName(jobName) : null;
    }

    public static boolean PlayerHasJobWithAttribute(String uuid, JobAttributeType attr) {
        return PlayerIsEmployed(uuid) ? GetPlayerData(uuid).playerHasJobWithAttribute(attr) : false;
    }

    public static PlayerJob GetPlayerJobByAttribute(String uuid, JobAttributeType attr) {
        return PlayerIsEmployed(uuid) ? GetPlayerData(uuid).getPlayerJobByAttribute(attr) : null;
    }

    public static boolean RemovePlayerJob(String uuid, String jobName) {
        boolean success = false;
        if (SQLManager.Connect()) {
            if (PlayerIsEmployed(uuid) && PlayerHasJob(uuid, jobName)) {
                try {
                    PreparedStatement removeStatement = SQLManager.GetConnection().prepareStatement("DELETE FROM UserData WHERE uuid=? AND job=?;");
                    removeStatement.setString(1, uuid);
                    removeStatement.setString(2, jobName);
                    removeStatement.executeUpdate();
                    removeStatement.close();
                    PlayerJob quitJob = ((PlayerJobData)playerData.get(GetPlayerIndex(uuid))).getPlayerJobByName(jobName);
                    quitJob.setLevel((int)Math.floor((double)quitJob.getLevel() * (1.0 - QuitJobDecreaseRate)));
                    quitJob.setExperiencePoints(0);
                    PreparedStatement updateJobDataStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?;");
                    updateJobDataStatement.setInt(1, quitJob.getLevel());
                    updateJobDataStatement.setInt(2, quitJob.getExperience());
                    updateJobDataStatement.setString(3, uuid);
                    updateJobDataStatement.setString(4, jobName);
                    updateJobDataStatement.executeUpdate();
                    updateJobDataStatement.close();
                } catch (SQLException var6) {
                    var6.printStackTrace();
                }

                ((PlayerJobData)playerData.get(GetPlayerIndex(uuid))).removeJobByName(jobName);
                if (((PlayerJobData)playerData.get(GetPlayerIndex(uuid))).getJobCount() == 0) {
                    playerData.removeIf((p) -> {
                        return p.getUuid().equals(uuid);
                    });
                }

                success = true;
            }

            SQLManager.Disconnect();
        }

        return success;
    }

    public static void AddLevelToPlayer(String uuid, String jobName, int amount) {
        if (SQLManager.Connect()) {
            if (PlayerIsEmployed(uuid)) {
                int idx = GetPlayerIndex(uuid);
                if (idx >= 0) {
                    int jobIdx = GetPlayerData(uuid).getJobIndexByName(jobName);
                    if (jobIdx >= 0) {
                        ((PlayerJobData)playerData.get(idx)).addLevelToJobByIndex(jobIdx, amount);
                        PlayerJob _plyJob = ((PlayerJobData)playerData.get(idx)).getJobByIndex(jobIdx);

                        try {
                            PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?");
                            updateStatement.setInt(1, _plyJob.getLevel());
                            updateStatement.setInt(2, _plyJob.getExperience());
                            updateStatement.setString(3, uuid);
                            updateStatement.setString(4, jobName);
                            updateStatement.executeUpdate();
                            updateStatement.close();
                        } catch (SQLException var7) {
                            var7.printStackTrace();
                        }
                    }
                }
            }

            SQLManager.Disconnect();
        }

    }

    public static void SetPlayerExperience(String uuid, String jobName, int experience) {
        if (SQLManager.Connect()) {
            if (PlayerIsEmployed(uuid)) {
                int idx = GetPlayerIndex(uuid);
                if (idx >= 0) {
                    int jobIdx = ((PlayerJobData)playerData.get(idx)).getJobIndexByName(jobName);
                    if (jobIdx >= 0) {
                        ((PlayerJobData)playerData.get(idx)).setJobExperienceByIndex(jobIdx, experience);
                        PlayerJob _plyJob = ((PlayerJobData)playerData.get(idx)).getJobByIndex(jobIdx);

                        try {
                            PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?;");
                            updateStatement.setInt(1, _plyJob.getLevel());
                            updateStatement.setInt(2, _plyJob.getExperience());
                            updateStatement.setString(3, uuid);
                            updateStatement.setString(4, jobName);
                            updateStatement.executeUpdate();
                            updateStatement.close();
                        } catch (SQLException var7) {
                            var7.printStackTrace();
                        }
                    }
                }
            }

            SQLManager.Disconnect();
        }

    }

    public static void SetPlayerLevel(String uuid, String job, int level) {
        if (SQLManager.Connect()) {
            if (PlayerIsEmployed(uuid)) {
                int idx = GetPlayerIndex(uuid);
                if (idx >= 0) {
                    int jobIdx = ((PlayerJobData)playerData.get(idx)).getJobIndexByName(job);
                    if (jobIdx >= 0) {
                        ((PlayerJobData)playerData.get(idx)).setJobLevelByIndex(jobIdx, level);
                        PlayerJob _plyJob = ((PlayerJobData)playerData.get(idx)).getJobByIndex(jobIdx);
                        if (_plyJob == null) {
                            return;
                        }

                        try {
                            PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?");
                            updateStatement.setInt(1, _plyJob.getLevel());
                            updateStatement.setInt(2, _plyJob.getExperience());
                            updateStatement.setString(3, uuid);
                            updateStatement.executeUpdate();
                            updateStatement.close();
                        } catch (SQLException var7) {
                            var7.printStackTrace();
                        }
                    }
                }
            }

            SQLManager.Disconnect();
        }

    }

    public static void AddExperiencePointsToPlayer(String uuid, String jobName, int amount) {
        if (SQLManager.Connect()) {
            if (PlayerIsEmployed(uuid)) {
                int idx = GetPlayerIndex(uuid);
                if (idx < 0) {
                    return;
                }

                int jobIdx = ((PlayerJobData)playerData.get(idx)).getJobIndexByName(jobName);
                if (jobIdx >= 0) {
                    ((PlayerJobData)playerData.get(idx)).addExperienceToJobByIndex(jobIdx, amount);
                    PlayerJob _plyJob = ((PlayerJobData)playerData.get(idx)).getJobByIndex(jobIdx);

                    try {
                        PreparedStatement updateStatement = SQLManager.GetConnection().prepareStatement("UPDATE JobData SET level=?, experience=? WHERE uuid=? AND job=?;");
                        updateStatement.setInt(1, _plyJob.getLevel());
                        updateStatement.setInt(2, _plyJob.getExperience());
                        updateStatement.setString(3, uuid);
                        updateStatement.setString(4, jobName);
                        updateStatement.executeUpdate();
                        updateStatement.close();
                    } catch (SQLException var7) {
                        var7.printStackTrace();
                    }
                }
            }

            SQLManager.Disconnect();
        }

    }

    public static PlayerJobData GetPlayerData(String uuid) {
        if (PlayerIsEmployed(uuid)) {
            int idx = GetPlayerIndex(uuid);
            return idx < 0 ? null : (PlayerJobData)playerData.get(idx);
        } else {
            return null;
        }
    }

    private static int GetJobIndex(String name) {
        int idx = -1;

        for(int i = 0; i < jobs.size() && idx < 0; ++i) {
            if (((Job)jobs.get(i)).getName().toLowerCase().equals(name.toLowerCase())) {
                idx = i;
            }
        }

        return idx;
    }

    public static Job GetJobByName(String name) {
        int idx = GetJobIndex(name);
        return idx >= 0 ? (Job)jobs.get(idx) : null;
    }

    public static List<PlayerJobData> GetPlayersByJob(String name) {
        return (List)playerData.stream().filter((p) -> {
            return p.playerHasJob(name);
        }).collect(Collectors.toList());
    }

    public static boolean JobExists(String name) {
        return jobs.stream().anyMatch((j) -> {
            return j.getName().toLowerCase().equals(name.toLowerCase());
        });
    }

    public static Job GetJobBySelectionIcon(ItemStack target) {
        Job _j = null;
        Iterator var2 = jobs.iterator();

        while(var2.hasNext()) {
            Job job = (Job)var2.next();
            if (job.getSelectionIcon().equals(target)) {
                _j = job;
            }
        }

        return _j;
    }
}
