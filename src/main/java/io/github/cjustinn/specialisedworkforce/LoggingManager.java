package io.github.cjustinn.specialisedworkforce;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class LoggingManager {
    public LoggingManager() {
    }

    public static CoreProtectAPI GetCoreProtect() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");
        if (plugin != null && plugin instanceof CoreProtect) {
            CoreProtectAPI coreProtect = ((CoreProtect)plugin).getAPI();
            if (!coreProtect.isEnabled()) {
                return null;
            } else {
                return coreProtect.APIVersion() < 9 ? null : coreProtect;
            }
        } else {
            return null;
        }
    }
}
