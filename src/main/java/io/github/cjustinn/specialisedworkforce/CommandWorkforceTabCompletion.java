package io.github.cjustinn.specialisedworkforce;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CommandWorkforceTabCompletion implements TabCompleter {
    public CommandWorkforceTabCompletion() {
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> results = new ArrayList();
        if (args.length == 1) {
            results.add("join");
            results.add("quit");
            results.add("status");
            results.add("leaderboard");
        } else {
            Iterator var9;
            Job job;
            if (args.length == 2 && args[0].equals("join")) {
                var9 = WorkforceManager.jobs.iterator();

                while(true) {
                    while(var9.hasNext()) {
                        job = (Job)var9.next();
                        if (!args[1].isEmpty() && job.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            results.add(job.getName());
                        } else if (args[1].isEmpty()) {
                            results.add(job.getName());
                        }
                    }

                    return results;
                }
            } else if (args.length == 2 && args[0].equals("quit")) {
                if (sender instanceof Player) {
                    Player ply = (Player)sender;
                    if (WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString())) {
                        Iterator var7 = WorkforceManager.GetPlayerData(ply.getUniqueId().toString()).getJobs().iterator();

                        while(var7.hasNext()) {
                            PlayerJob pJob = (PlayerJob)var7.next();
                            results.add(pJob.getJob().getName());
                        }
                    }
                } else {
                    var9 = WorkforceManager.jobs.iterator();

                    while(var9.hasNext()) {
                        job = (Job)var9.next();
                        results.add(job.getName());
                    }
                }
            }
        }

        return results;
    }
}
