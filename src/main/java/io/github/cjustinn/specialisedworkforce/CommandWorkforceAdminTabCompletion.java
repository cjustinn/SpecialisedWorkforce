package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandWorkforceAdminTabCompletion implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> results = new ArrayList<String>();

        if (args.length == 1) {
            results.add("add");
            results.add("set");
        } else if (args.length == 2 && (args[0].equals("add") || args[0].equals("set"))) {
            results.add("experience");
            results.add("levels");
        } else if (args.length == 3 && (args[0].equals("add") || args[0].equals("set"))) {
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                results.add(p.getName());
            }
        } else if (args.length == 4 && (args[0].equals("add") || args[0].equals("set"))) {
            Player ply = Bukkit.getPlayer(args[2]);
            if (ply != null) {
                if (WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString())) {
                    List<PlayerJob> plyJobs = WorkforceManager.GetPlayerData(ply.getUniqueId().toString()).getJobs();
                    for (PlayerJob job : plyJobs) {
                        results.add(job.getJob().getName());
                    }
                }
            } else {
                for (Job job : WorkforceManager.jobs) {
                    results.add(job.getName());
                }
            }
        }

        return results;
    }
}
