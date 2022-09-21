package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PlayerJob {

    private String uuid;
    private Job job;
    private int level;
    private int experience;

    public PlayerJob(String _uuid, Job _job, int _level, int _exp) {
        this.uuid = _uuid;
        this.job = _job;
        this.level = _level;
        this.experience = _exp;
    }

    public Job getJob() { return this.job; }
    public int getLevel() { return this.level; }
    public int getExperience() { return this.experience; }

    public ItemStack GetStatusIcon() {
        ItemStack statusIcon = new ItemStack(getJob().getIcon(), 1);

        ItemMeta statusMeta = statusIcon.getItemMeta();
        statusMeta.setDisplayName(String.format("§6§l%s§r", getJob().getName()));

        String[] desc = new String[] {
                "",
                String.format("§7Level §6%d§r", getLevel()),
                getLevel() >= WorkforceManager.MaximumJobLevel ? "§6Max. Level§r" : String.format("§6%d §7/ %d§r", getExperience(), WorkforceManager.GetRequiredExperienceAtLevel(getLevel())),
                "",
                String.format("§cRight-click to quit§r")
        };

        List<String> descList = new ArrayList<>();
        for (String s : desc) {
            descList.add(s);
        }
        statusMeta.setLore(descList);

        statusIcon.setItemMeta(statusMeta);

        return statusIcon;
    }

    // Increment the player's level by the amount provided, safely preventing them from exceeding the config-defined maximum job level.
    public void incrementLevel(int amount) {
        if (this.level >= WorkforceManager.MaximumJobLevel && amount >= 0) return;
        else if (this.level == 1 && amount <= 0) return;
        else if (amount == 0) return;

        if (this.level <= WorkforceManager.MaximumJobLevel) {
            boolean didReachMax = false;

            if (this.level + amount >= WorkforceManager.MaximumJobLevel) {
                if (this.level != WorkforceManager.MaximumJobLevel) {
                    didReachMax = true;
                }

                this.level = WorkforceManager.MaximumJobLevel;
            } else if (this.level + amount < 1){
                this.level = 1;
            } else {
                this.level += amount;
            }

            Player ply = Bukkit.getPlayer(UUID.fromString(this.uuid));
            if (ply != null) {
                ply.sendMessage("You are now a " + ChatColor.GOLD + String.format("Level %d %s", this.level, this.job.getName()) + ChatColor.RESET + "!");
            }

            if (didReachMax) {
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + Bukkit.getServer().getPlayer(UUID.fromString(this.uuid)).getName() + ChatColor.RESET + " is now a maximum level " + ChatColor.GOLD + this.job.getName() + ChatColor.RESET + "!");
            }
        }
    }

    // Safely increment the player's current experience points. If it exceeds the required exp. for the next level, it will increment the level and decrement the experience points before updating the stored value.
    public void incrementExperiencePoints(int _exp) {
        if (getLevel() >= WorkforceManager.MaximumJobLevel) return;
        else {
            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

            boolean hasLevelledUp = false;
            int totalExp = _exp + this.experience;

            // Get the experience requirements for the next level of the current job.
            String requiredExpEq = WorkforceManager.experienceRequirementEquation.replace("{level}", String.format("%d", getLevel()));
            int expReq;

            // If totalExp is greater than the requirement, level the user up-- repeat until totalExp < requiredExp.
            do {
                try {
                    expReq = new BigDecimal(sEngine.eval(requiredExpEq).toString()).intValue();
                } catch(ScriptException e) {
                    e.printStackTrace();

                    return;
                }

                if (totalExp >= expReq) {
                    totalExp -= expReq;
                    incrementLevel(1);

                    requiredExpEq = WorkforceManager.experienceRequirementEquation.replace("{level}", String.format("%d", getLevel()));
                }
            } while (totalExp >= expReq);

            // Update this.experiencePoints to the remaining totalExp value.
            this.experience = totalExp;
        }
    }

    // Unsafely set the player's level directly-- can bypass the config-defined maximum level.
    public void setLevel(int _level) { this.level = _level < 1 ? 1 : _level; }
    public void setExperiencePoints(int _exp) { this.experience = _exp < 0 ? 0 : _exp; }
}
