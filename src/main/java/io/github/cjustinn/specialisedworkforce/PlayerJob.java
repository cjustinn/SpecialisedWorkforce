package io.github.cjustinn.specialisedworkforce;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

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

    public Job getJob() {
        return this.job;
    }

    public int getLevel() {
        return this.level;
    }

    public int getExperience() {
        return this.experience;
    }

    public ItemStack GetStatusIcon() {
        ItemStack statusIcon = new ItemStack(this.getJob().getIcon(), 1);
        ItemMeta statusMeta = statusIcon.getItemMeta();
        statusMeta.setDisplayName(String.format("§6§l%s§r", this.getJob().getName()));

        List<String> descList = new ArrayList();

        // Add job description.
        for (String line : this.job.getDescription()) {
            descList.add(line);
        }

        // Add spacer.
        descList.add("");

        // Add player stats data.
        descList.add(String.format("§7Level §6%d§r", this.getLevel()));
        descList.add(this.getLevel() >= WorkforceManager.MaximumJobLevel ? "§6Max. Level§r" : String.format("§6%d §7/ %d§r", this.getExperience(), WorkforceManager.GetRequiredExperienceAtLevel(this.getLevel())));

        // Add spacer.
        descList.add("");

        // Add quit info.
        descList.add(String.format("§cRight-click to quit§r"));

        statusMeta.setLore(descList);
        statusIcon.setItemMeta(statusMeta);
        return statusIcon;
    }

    public void incrementLevel(int amount) {
        if (this.level < WorkforceManager.MaximumJobLevel || amount < 0) {
            if (this.level != 1 || amount > 0) {
                if (amount != 0) {
                    if (this.level <= WorkforceManager.MaximumJobLevel) {
                        boolean didReachMax = false;
                        if (this.level + amount >= WorkforceManager.MaximumJobLevel) {
                            if (this.level != WorkforceManager.MaximumJobLevel) {
                                didReachMax = true;
                            }

                            this.level = WorkforceManager.MaximumJobLevel;
                        } else if (this.level + amount < 1) {
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
            }
        }
    }

    public void incrementExperiencePoints(int _exp) {
        if (this.getLevel() < WorkforceManager.MaximumJobLevel) {
            ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
            boolean hasLevelledUp = false;
            int totalExp = _exp + this.experience;
            String requiredExpEq = WorkforceManager.experienceRequirementEquation.replace("{level}", String.format("%d", this.getLevel()));

            int expReq;
            do {
                try {
                    expReq = (new BigDecimal(sEngine.eval(requiredExpEq).toString())).intValue();
                } catch (ScriptException var8) {
                    var8.printStackTrace();
                    return;
                }

                if (totalExp >= expReq) {
                    totalExp -= expReq;
                    this.incrementLevel(1);
                    requiredExpEq = WorkforceManager.experienceRequirementEquation.replace("{level}", String.format("%d", this.getLevel()));
                }
            } while(totalExp >= expReq);

            this.experience = totalExp;
        }
    }

    public void setLevel(int _level) {
        this.level = _level < 1 ? 1 : _level;
    }

    public void setExperiencePoints(int _exp) {
        this.experience = _exp < 0 ? 0 : _exp;
    }
}
