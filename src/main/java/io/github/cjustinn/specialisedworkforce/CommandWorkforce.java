package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CommandWorkforce implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getConsoleSender().sendMessage(String.format("[%s] Command %s cannot be run by the console.", "SpecialisedWorkforce", command.getName()));
        } else {
            if (args.length > 0) {
                String plyUuid = ((Player) sender).getUniqueId().toString();

                // Check sub-command.
                // workforce join <job>
                if (args[0].toLowerCase().equals("join")) {
                    if (WorkforceManager.PlayerIsEmployed(plyUuid) && !WorkforceManager.PlayerBelowMaxJobs(plyUuid)) {
                        sender.sendMessage(ChatColor.DARK_RED + "You already have the maximum number of jobs!");
                    } else if (args.length < 2) {
                        ((Player) sender).openInventory(CustomInventoryManager.GetJobJoinInventory());
                    } else {

                        if (WorkforceManager.JobExists(args[1])) {
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

                    }
                }
                // workforce quit
                else if (args[0].toLowerCase().equals("quit")) {
                    if (!WorkforceManager.PlayerIsEmployed(plyUuid)) {
                        sender.sendMessage(ChatColor.DARK_RED + "You do not have a job to quit!");
                    } else if (args.length < 2) {
                        ((Player) sender).openInventory(GetUserJobList((Player) sender));
                    } else {
                        if (WorkforceManager.JobExists(args[1])) {

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
                    }
                }
                // /workforce status
                else if (args[0].toLowerCase().equals("status")) {
                    if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
                        List<PlayerJob> jobs = WorkforceManager.GetPlayerData(plyUuid).getJobs();
                        for (PlayerJob _j : jobs) {
                            ((Player) sender).sendMessage(String.format("%sYou are a %sLevel %d %s%s (%s)", ChatColor.GRAY, ChatColor.GOLD, _j.getLevel(), _j.getJob().getName(), ChatColor.GRAY, _j.getLevel() >= WorkforceManager.MaximumJobLevel ? "Max. Level" : String.format("%d/%d", _j.getExperience(), WorkforceManager.GetRequiredExperienceAtLevel(_j.getLevel()))));
                        }
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "You are unemployed!");
                        sender.sendMessage("");
                        sender.sendMessage(String.format("%sView available jobs with %s/workforce join%s!", ChatColor.GRAY, ChatColor.GOLD, ChatColor.RESET));
                    }
                }
            } else {
                // Report back to the player about their job status.
                if (WorkforceManager.PlayerIsEmployed(((Player) sender).getUniqueId().toString())) {
                    ((Player) sender).openInventory(GetUserJobList((Player) sender));
                } else {

                    sender.sendMessage(ChatColor.DARK_RED + "You are unemployed!");
                    sender.sendMessage("");
                    sender.sendMessage(String.format("%sView available jobs with %s/workforce join%s!", ChatColor.GRAY, ChatColor.GOLD, ChatColor.RESET));

                }
            }
        }

        return true;
    }

    private Inventory GetUserJobList(Player ply) {
        PlayerJobData _ply = WorkforceManager.GetPlayerData(ply.getUniqueId().toString());
        List<PlayerJob> plyJobs = _ply.getJobs();

        final int rowsToFill = (int) Math.ceil(_ply.getJobCount() / 9.0);
        Inventory playerJobsInv = Bukkit.createInventory(null, (rowsToFill * 9), CustomInventoryManager.employedJobsViewInvName);

        for (int i = 0; i < rowsToFill; i++) {
            int startIdx = 0 + (9 * i);
            int endIdx = startIdx + 8;

            if (endIdx >= plyJobs.size())
                endIdx = plyJobs.size() - 1;

            int itemsInRow = (endIdx - startIdx) + 1;
            final int startingSlot = (9 * i) + CustomInventoryManager.GetRowStartingSlot(itemsInRow);

            int currentSlot = startingSlot;

            for (int j = startIdx; j <= endIdx; j++) {
                if (itemsInRow % 2 == 0 && currentSlot == ((9 * i) + 4)) {
                    currentSlot++;
                }

                playerJobsInv.setItem(currentSlot, plyJobs.get(j).GetStatusIcon());
                currentSlot++;
            }
        }

        return playerJobsInv;
    }
}
