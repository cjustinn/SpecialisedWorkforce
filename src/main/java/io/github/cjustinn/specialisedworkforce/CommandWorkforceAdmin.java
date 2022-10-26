package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandWorkforceAdmin implements CommandExecutor {
    public CommandWorkforceAdmin() {
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getConsoleSender().sendMessage(String.format("[%s] Command %s cannot be run by the console.", "SpecialisedWorkforce", command.getName()));
        } else if (args.length > 0 && (args[0].toLowerCase().equals("add") || args[0].toLowerCase().equals("set"))) {
            if (args.length >= 5) {
                Player ply = Bukkit.getPlayer(args[2]);
                if (ply != null) {
                    if (!WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString())) {
                        ((Player)sender).sendMessage(ChatColor.DARK_RED + "The provided player is not employed.");
                        return true;
                    }

                    if (!WorkforceManager.JobExists(args[3])) {
                        ((Player)sender).sendMessage(String.format("%s%s is not a valid job.", ChatColor.DARK_RED, args[3].toUpperCase()));
                        return true;
                    }

                    if (WorkforceManager.JobExists(args[3]) && !WorkforceManager.PlayerHasJob(((Player)sender).getUniqueId().toString(), args[3])) {
                        ((Player)sender).sendMessage(String.format("%s%s is not a%s %s.", ChatColor.DARK_RED, args[2].toUpperCase(), this.StringStartsWithVowel(args[3]) ? 'n' : "", args[3].toUpperCase()));
                        return true;
                    }

                    int amount = 0;

                    try {
                        amount = Integer.parseInt(args[4]);
                    } catch (NumberFormatException var8) {
                        var8.printStackTrace();
                    }

                    if (args[1].toLowerCase().equals("levels")) {
                        if (args[0].equals("add")) {
                            WorkforceManager.AddLevelToPlayer(ply.getUniqueId().toString(), args[3], amount);
                            ((Player)sender).sendMessage(ChatColor.GREEN + String.format("You have changed %s's %s levels by %s.", args[2], args[3].toUpperCase(), args[4]));
                        } else {
                            WorkforceManager.SetPlayerLevel(ply.getUniqueId().toString(), args[3], amount);
                            ((Player)sender).sendMessage(ChatColor.GREEN + String.format("You have set %s's %s level to %s.", args[2], args[3].toUpperCase(), args[4]));
                        }
                    } else {
                        if (!args[1].toLowerCase().equals("experience")) {
                            ((Player)sender).sendMessage(ChatColor.DARK_RED + "You have provided an invalid incrementation type. Valid options are 'levels' or 'experience'.");
                            return true;
                        }

                        if (args[0].equals("add")) {
                            WorkforceManager.AddExperiencePointsToPlayer(ply.getUniqueId().toString(), args[3], amount);
                            ((Player)sender).sendMessage(ChatColor.GREEN + String.format("You have changed %s's %s experience by %s.", args[2], args[3].toUpperCase(), args[4]));
                        } else {
                            WorkforceManager.SetPlayerExperience(ply.getUniqueId().toString(), args[3], amount);
                            ((Player)sender).sendMessage(ChatColor.GREEN + String.format("You have set %s's %s experience to %s.", args[2], args[3].toUpperCase(), args[4]));
                        }
                    }
                } else {
                    ((Player)sender).sendMessage(ChatColor.DARK_RED + String.format("%s is not a valid user.", args[2]));
                }

                return true;
            }

            ((Player)sender).sendMessage(ChatColor.DARK_RED + "You must provide valid command arguments.");
        }

        return true;
    }

    private boolean StringStartsWithVowel(String target) {
        boolean startsWithVowel = false;
        char beginning = target.toLowerCase().charAt(0);
        char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
        char[] var5 = vowels;
        int var6 = vowels.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            char c = var5[var7];
            if (beginning == c) {
                startsWithVowel = true;
            }
        }

        return startsWithVowel;
    }
}
