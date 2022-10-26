package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CustomInventoryListener implements Listener {
    public CustomInventoryListener() {
    }

    @EventHandler
    public void InventoryWasClicked(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(CustomInventoryManager.jobSelectionInvName) || event.getView().getTitle().equals(CustomInventoryManager.employedJobsViewInvName) || event.getView().getTitle().equals(CustomInventoryManager.leaderboardInvName)) {
            if (event.getCurrentItem().getType().isAir()) {
                return;
            }

            String plyUuid = ((Player)event.getWhoClicked()).getUniqueId().toString();
            if (event.getView().getTitle().equals(CustomInventoryManager.employedJobsViewInvName) && event.isRightClick()) {
                PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
                PlayerJob plyJob = ply.getPlayerJobByStatusIcon(event.getCurrentItem());
                if (plyJob != null) {
                    WorkforceManager.RemovePlayerJob(plyUuid, plyJob.getJob().getName());
                    event.getWhoClicked().sendMessage(String.format("%sYou have quit your job as a %s!", ChatColor.GREEN, plyJob.getJob().getName()));
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce"), () -> {
                        event.getWhoClicked().closeInventory();
                    });
                }
            } else if (event.getView().getTitle().equals(CustomInventoryManager.jobSelectionInvName) && event.isLeftClick()) {
                Job targetJob = WorkforceManager.GetJobBySelectionIcon(event.getCurrentItem());
                if (targetJob != null) {
                    if (WorkforceManager.PlayerIsEmployed(plyUuid) && !WorkforceManager.PlayerBelowMaxJobs(plyUuid)) {
                        event.getWhoClicked().sendMessage(String.format("%sYou already have %d job%s!", ChatColor.DARK_RED, WorkforceManager.maxPlayerJobs, WorkforceManager.maxPlayerJobs == 1 ? "" : "s"));
                    } else if (WorkforceManager.PlayerHasJob(plyUuid, targetJob.getName())) {
                        event.getWhoClicked().sendMessage(String.format("%sYou already have that job!", ChatColor.DARK_RED));
                    } else if (WorkforceManager.AssignJobToPlayer(plyUuid, targetJob.getName())) {
                        event.getWhoClicked().sendMessage(String.format("%sYou are now a %s!", ChatColor.GREEN, targetJob.getName()));
                    } else {
                        event.getWhoClicked().sendMessage(String.format("%sThere was an error joining the job!", ChatColor.DARK_RED));
                    }

                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce"), () -> {
                        event.getWhoClicked().closeInventory();
                    });
                }
            }

            event.setCancelled(true);
        }

    }
}
