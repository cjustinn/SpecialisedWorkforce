package io.github.cjustinn.specialisedworkforce;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandWorkforceTabCompletion implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> results = new ArrayList<String>();

        if (args.length == 1) {
            results.add("join");
            results.add("quit");
            results.add("status");
        } else if (args.length == 2 && (args[0].equals("join"))) {
            for (Job job : WorkforceManager.jobs) {
                if (!args[1].isEmpty() && job.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    results.add(job.getName());
                } else if (args[1].isEmpty()) {
                    results.add(job.getName());
                }
            }
        } else if (args.length == 2 && (args[0].equals("quit"))) {
            if (sender instanceof Player) {
                Player ply = (Player) sender;

                if (WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString())) {
                    for (PlayerJob job : WorkforceManager.GetPlayerData(ply.getUniqueId().toString()).getJobs()) {
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
