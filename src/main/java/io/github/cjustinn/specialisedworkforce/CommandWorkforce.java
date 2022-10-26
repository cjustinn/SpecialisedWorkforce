package io.github.cjustinn.specialisedworkforce;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CommandWorkforce implements CommandExecutor {
    public CommandWorkforce() {
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getConsoleSender().sendMessage(String.format("[%s] Command %s cannot be run by the console.", "SpecialisedWorkforce", command.getName()));
        } else if (args.length > 0) {
            String plyUuid = ((Player)sender).getUniqueId().toString();
            if (args[0].toLowerCase().equals("join")) {
                if (WorkforceManager.PlayerIsEmployed(plyUuid) && !WorkforceManager.PlayerBelowMaxJobs(plyUuid)) {
                    sender.sendMessage(ChatColor.DARK_RED + "You already have the maximum number of jobs!");
                } else if (args.length < 2) {
                    ((Player)sender).openInventory(CustomInventoryManager.GetJobJoinInventory());
                } else if (WorkforceManager.JobExists(args[1])) {
                    if (WorkforceManager.PlayerHasJob(plyUuid, args[1])) {
                        sender.sendMessage(ChatColor.DARK_RED + "You already have that job!");
                    } else if (WorkforceManager.AssignJobToPlayer(plyUuid, args[1])) {
                        sender.sendMessage(ChatColor.GREEN + "You have become a " + args[1] + "!");
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "There was an error joining the job!");
                    }
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + args[1].toUpperCase() + " is not a valid job name.");
                }
            } else if (args[0].toLowerCase().equals("quit")) {
                if (!WorkforceManager.PlayerIsEmployed(plyUuid)) {
                    sender.sendMessage(ChatColor.DARK_RED + "You do not have a job to quit!");
                } else if (args.length < 2) {
                    ((Player)sender).openInventory(this.GetUserJobList((Player)sender));
                } else if (WorkforceManager.JobExists(args[1])) {
                    if (WorkforceManager.PlayerHasJob(plyUuid, args[1])) {
                        if (WorkforceManager.RemovePlayerJob(plyUuid, args[1])) {
                            sender.sendMessage(String.format("%sYou have quit your job as a %s!", ChatColor.GREEN, args[1].toUpperCase()));
                        } else {
                            sender.sendMessage(ChatColor.DARK_RED + "There was an error quitting your job! Please try again.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "You cannot quit a job that you do not have!");
                    }
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + args[1].toUpperCase() + " is not a valid job name.");
                }
            } else if (args[0].toLowerCase().equals("status")) {
                if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
                    List<PlayerJob> jobs = WorkforceManager.GetPlayerData(plyUuid).getJobs();
                    Iterator var7 = jobs.iterator();

                    while(var7.hasNext()) {
                        PlayerJob _j = (PlayerJob)var7.next();
                        ((Player)sender).sendMessage(String.format("%sYou are a %sLevel %d %s%s (%s)", ChatColor.GRAY, ChatColor.GOLD, _j.getLevel(), _j.getJob().getName(), ChatColor.GRAY, _j.getLevel() >= WorkforceManager.MaximumJobLevel ? "Max. Level" : String.format("%d/%d", _j.getExperience(), WorkforceManager.GetRequiredExperienceAtLevel(_j.getLevel()))));
                    }
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "You are unemployed!");
                    sender.sendMessage("");
                    sender.sendMessage(String.format("%sView available jobs with %s/workforce join%s!", ChatColor.GRAY, ChatColor.GOLD, ChatColor.RESET));
                }
            } else if (args[0].toLowerCase().equals("leaderboard")) {
                try {
                    ((Player) sender).openInventory(this.GetLeaderboardInv());
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (WorkforceManager.PlayerIsEmployed(((Player)sender).getUniqueId().toString())) {
            ((Player)sender).openInventory(this.GetUserJobList((Player)sender));
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "You are unemployed!");
            sender.sendMessage("");
            sender.sendMessage(String.format("%sView available jobs with %s/workforce join%s!", ChatColor.GRAY, ChatColor.GOLD, ChatColor.RESET));
        }

        return true;
    }

    private Inventory GetLeaderboardInv() throws ExecutionException, InterruptedException {
        int rowsToFill = (int) Math.ceil((double) WorkforceManager.playerData.size() / 9.0);
        Inventory leaderboardInv = Bukkit.createInventory(null, rowsToFill * 9, CustomInventoryManager.leaderboardInvName);

        for (int i = 0; i < rowsToFill; i++) {
            int startIdx = 0 + 9 * i;
            int endIdx = startIdx + 8;

            if (endIdx >= WorkforceManager.playerData.size())
                endIdx = WorkforceManager.playerData.size() - 1;

            int itemsInRow = endIdx - startIdx + 1;
            int startingSlot = 9 * i + (CustomInventoryManager.GetRowStartingSlot(itemsInRow));
            int currentSlot = startingSlot;

            for (int j = startIdx; j <= endIdx; j++) {
                if (itemsInRow % 2 == 0 && currentSlot == 9 * i + 4)
                    currentSlot++;

                OfflinePlayer ply = Bukkit.getOfflinePlayer(WorkforceManager.playerData.get(j).getUuid());
                Player plyObj = ply.getPlayer();

                ItemStack plyStats = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta plyStatsMeta = (SkullMeta) plyStats.getItemMeta();

                //plyStatsMeta.setOwningPlayer(ply);
                plyStatsMeta.setOwnerProfile(ply.getPlayerProfile().update().get());
                // Set skull skin.
                //if (plyObj != null)
                //    plyStatsMeta.setOwnerProfile(plyObj.getPlayerProfile());

                // Set description.
                List<String> desc = new ArrayList<String>();

                desc.add("");

                for (PlayerJob plyJob : WorkforceManager.playerData.get(j).getJobs()) {
                    desc.add(String.format("§7Level§r §6%d %s§r", plyJob.getLevel(), plyJob.getJob().getName()));
                }

                plyStatsMeta.setLore(desc);
                plyStatsMeta.setDisplayName(String.format("§6%s", ply.getPlayerProfile().update().get().getName()));

                plyStats.setItemMeta(plyStatsMeta);

                leaderboardInv.setItem(currentSlot, plyStats);

                currentSlot++;
            }
        }

        return leaderboardInv;
    }

    private Inventory GetUserJobList(Player ply) {
        PlayerJobData _ply = WorkforceManager.GetPlayerData(ply.getUniqueId().toString());
        List<PlayerJob> plyJobs = _ply.getJobs();
        int rowsToFill = (int)Math.ceil((double)_ply.getJobCount() / 9.0);
        Inventory playerJobsInv = Bukkit.createInventory((InventoryHolder)null, rowsToFill * 9, CustomInventoryManager.employedJobsViewInvName);

        for(int i = 0; i < rowsToFill; ++i) {
            int startIdx = 0 + 9 * i;
            int endIdx = startIdx + 8;
            if (endIdx >= plyJobs.size()) {
                endIdx = plyJobs.size() - 1;
            }

            int itemsInRow = endIdx - startIdx + 1;
            int startingSlot = 9 * i + CustomInventoryManager.GetRowStartingSlot(itemsInRow);
            int currentSlot = startingSlot;

            for(int j = startIdx; j <= endIdx; ++j) {
                if (itemsInRow % 2 == 0 && currentSlot == 9 * i + 4) {
                    ++currentSlot;
                }

                playerJobsInv.setItem(currentSlot, ((PlayerJob)plyJobs.get(j)).GetStatusIcon());
                ++currentSlot;
            }
        }

        return playerJobsInv;
    }
}
