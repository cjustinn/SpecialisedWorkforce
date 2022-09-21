package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PlayerJobData {
    // Data member definitions.
    private String uuid;
    private List<PlayerJob> jobs;

    // Constructor(s)
    public PlayerJobData(String _uuid, List<PlayerJob> _jobs) {
        this.uuid = _uuid;
        this.jobs = _jobs;
    }

    // Getter functions for the data members.
    public String getUuid() { return this.uuid; }
    public int getJobCount() { return this.jobs.size(); }

    public List<PlayerJob> getJobs() { return this.jobs; }

    public boolean playerHasJob(String name) {
        return this.jobs.stream().anyMatch(j -> j.getJob().getName().equals(name));
    }

    public boolean playerHasJobWithAttribute(JobAttributeType attrType) {
        return this.jobs.stream().anyMatch(j -> j.getJob().hasJobAttribute(attrType) == true);
    }

    public PlayerJob getPlayerJobByStatusIcon(ItemStack target) {
        PlayerJob job = null;

        for (int i = 0; i < this.jobs.size(); i++) {
            if (this.jobs.get(i).GetStatusIcon().equals(target))
                job = this.jobs.get(i);
        }

        return job;
    }

    public PlayerJob getPlayerJobByAttribute(JobAttributeType attrType) {
        if (playerHasJobWithAttribute(attrType)) {
            return this.jobs.stream().filter(j -> j.getJob().hasJobAttribute(attrType) == true).findFirst().get();
        } else return null;
    }

    public void addJobToPlayer(PlayerJob job) {
        this.jobs.add(job);
    }

    public int getJobIndexByName(String name) {
        int idx = -1;

        for (int i = 0; i < this.jobs.size() && idx < 0; i++) {
            if (this.jobs.get(i).getJob().getName().toLowerCase().equals(name.toLowerCase()))
                idx = i;
        }

        return idx;
    }

    public PlayerJob getJobByIndex(int idx) {
        return this.jobs.get(idx);
    }

    public PlayerJob getPlayerJobByName(String jobName) {
        return this.jobs.get(getJobIndexByName(jobName));
    }

    public Job getJobByName(String name) {
        Job _job = null;

        int idx = getJobIndexByName(name);
        if (idx >= 0) {
            _job = this.jobs.get(idx).getJob();
        }

        return _job;
    }

    public void removeJobByName(String jobName) {
        this.jobs.removeIf(j -> j.getJob().getName().equals(jobName));
    }

    public void setJobLevelByIndex(int idx, int level) {
        this.jobs.get(idx).setLevel(level);
    }

    public void addLevelToJobByIndex(int idx, int amount) {
        this.jobs.get(idx).incrementLevel(amount);
    }

    public void setJobExperienceByIndex(int idx, int exp) {
        this.jobs.get(idx).setExperiencePoints(exp);
    }

    public void addExperienceToJobByIndex(int idx, int amount) {
        this.jobs.get(idx).incrementExperiencePoints(amount);
    }

}
