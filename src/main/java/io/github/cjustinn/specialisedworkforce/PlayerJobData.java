package io.github.cjustinn.specialisedworkforce;

import java.util.List;
import org.bukkit.inventory.ItemStack;

public class PlayerJobData {
    private String uuid;
    private List<PlayerJob> jobs;

    public PlayerJobData(String _uuid, List<PlayerJob> _jobs) {
        this.uuid = _uuid;
        this.jobs = _jobs;
    }

    public String getUuid() {
        return this.uuid;
    }

    public int getJobCount() {
        return this.jobs.size();
    }

    public List<PlayerJob> getJobs() {
        return this.jobs;
    }

    public boolean playerHasJob(String name) {
        return this.jobs.stream().anyMatch((j) -> {
            return j.getJob().getName().equals(name);
        });
    }

    public boolean playerHasJobWithAttribute(JobAttributeType attrType) {
        return this.jobs.stream().anyMatch((j) -> {
            return j.getJob().hasJobAttribute(attrType);
        });
    }

    public PlayerJob getPlayerJobByStatusIcon(ItemStack target) {
        PlayerJob job = null;

        for(int i = 0; i < this.jobs.size(); ++i) {
            if (((PlayerJob)this.jobs.get(i)).GetStatusIcon().equals(target)) {
                job = (PlayerJob)this.jobs.get(i);
            }
        }

        return job;
    }

    public PlayerJob getPlayerJobByAttribute(JobAttributeType attrType) {
        return this.playerHasJobWithAttribute(attrType) ? (PlayerJob)this.jobs.stream().filter((j) -> {
            return j.getJob().hasJobAttribute(attrType);
        }).findFirst().get() : null;
    }

    public void addJobToPlayer(PlayerJob job) {
        this.jobs.add(job);
    }

    public int getJobIndexByName(String name) {
        int idx = -1;

        for(int i = 0; i < this.jobs.size() && idx < 0; ++i) {
            if (((PlayerJob)this.jobs.get(i)).getJob().getName().toLowerCase().equals(name.toLowerCase())) {
                idx = i;
            }
        }

        return idx;
    }

    public PlayerJob getJobByIndex(int idx) {
        return (PlayerJob)this.jobs.get(idx);
    }

    public PlayerJob getPlayerJobByName(String jobName) {
        return (PlayerJob)this.jobs.get(this.getJobIndexByName(jobName));
    }

    public Job getJobByName(String name) {
        Job _job = null;
        int idx = this.getJobIndexByName(name);
        if (idx >= 0) {
            _job = ((PlayerJob)this.jobs.get(idx)).getJob();
        }

        return _job;
    }

    public void removeJobByName(String jobName) {
        this.jobs.removeIf((j) -> {
            return j.getJob().getName().equals(jobName);
        });
    }

    public void setJobLevelByIndex(int idx, int level) {
        ((PlayerJob)this.jobs.get(idx)).setLevel(level);
    }

    public void addLevelToJobByIndex(int idx, int amount) {
        ((PlayerJob)this.jobs.get(idx)).incrementLevel(amount);
    }

    public void setJobExperienceByIndex(int idx, int exp) {
        ((PlayerJob)this.jobs.get(idx)).setExperiencePoints(exp);
    }

    public void addExperienceToJobByIndex(int idx, int amount) {
        ((PlayerJob)this.jobs.get(idx)).incrementExperiencePoints(amount);
    }
}
