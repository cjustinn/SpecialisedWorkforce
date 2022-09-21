package io.github.cjustinn.specialisedworkforce;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class LoggingManager {

    public static CoreProtectAPI GetCoreProtect() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

        if (plugin == null || !(plugin instanceof CoreProtect))
            return null;

        CoreProtectAPI coreProtect = ((CoreProtect) plugin).getAPI();
        if (!coreProtect.isEnabled())
            return null;

        if (coreProtect.APIVersion() < 9)
            return null;

        return coreProtect;
    }

}
