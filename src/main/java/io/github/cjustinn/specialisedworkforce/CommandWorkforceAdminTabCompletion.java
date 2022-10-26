package io.github.cjustinn.specialisedworkforce;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CommandWorkforceAdminTabCompletion implements TabCompleter {
    public CommandWorkforceAdminTabCompletion() {
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> results = new ArrayList();
        if (args.length == 1) {
            results.add("add");
            results.add("set");
        } else if (args.length != 2 || !args[0].equals("add") && !args[0].equals("set")) {
            if (args.length != 3 || !args[0].equals("add") && !args[0].equals("set")) {
                if (args.length == 4 && (args[0].equals("add") || args[0].equals("set"))) {
                    Player ply = Bukkit.getPlayer(args[2]);
                    if (ply != null) {
                        if (WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString())) {
                            List<PlayerJob> plyJobs = WorkforceManager.GetPlayerData(ply.getUniqueId().toString()).getJobs();
                            Iterator var8 = plyJobs.iterator();

                            while(var8.hasNext()) {
                                PlayerJob job = (PlayerJob)var8.next();
                                results.add(job.getJob().getName());
                            }
                        }
                    } else {
                        Iterator var12 = WorkforceManager.jobs.iterator();

                        while(var12.hasNext()) {
                            Job job = (Job)var12.next();
                            results.add(job.getName());
                        }
                    }
                }
            } else {
                Iterator var6 = Bukkit.getServer().getOnlinePlayers().iterator();

                while(var6.hasNext()) {
                    Player p = (Player)var6.next();
                    results.add(p.getName());
                }
            }
        } else {
            results.add("experience");
            results.add("levels");
        }

        return results;
    }
}
