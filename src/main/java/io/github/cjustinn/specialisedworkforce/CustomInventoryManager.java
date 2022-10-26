package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomInventoryManager {
    public static String FillerItemName = "";
    public static String jobSelectionInvName = "Available Jobs";
    public static String employedJobsViewInvName = "Your Jobs";
    public static String leaderboardInvName = "Job Leaderboard";
    public static Inventory selectionInv = null;

    public CustomInventoryManager() {
    }

    public static ItemStack GetFillerItem() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS, 1);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(FillerItemName);
        filler.setItemMeta(fillerMeta);
        return filler;
    }

    public static Inventory GetJobJoinInventory() {
        if (selectionInv == null) {
            int rowsToFill = (int)Math.ceil((double)WorkforceManager.jobs.size() / 9.0);
            selectionInv = Bukkit.createInventory((InventoryHolder)null, rowsToFill * 9, jobSelectionInvName);

            for(int i = 0; i < rowsToFill; ++i) {
                int startIdx = 0 + 9 * i;
                int endIdx = startIdx + 8;
                if (endIdx >= WorkforceManager.jobs.size()) {
                    endIdx = WorkforceManager.jobs.size() - 1;
                }

                int itemsInRow = endIdx - startIdx + 1;
                int startingSlot = 9 * i + GetRowStartingSlot(itemsInRow);
                int currentSlot = startingSlot;

                for(int j = startIdx; j <= endIdx; ++j) {
                    if (itemsInRow % 2 == 0 && currentSlot == 9 * i + 4) {
                        ++currentSlot;
                    }

                    selectionInv.setItem(currentSlot, ((Job)WorkforceManager.jobs.get(j)).getSelectionIcon());
                    ++currentSlot;
                }
            }
        }

        return selectionInv;
    }

    public static int GetRowStartingSlot(int numberOfItems) {
        int slot;
        if (numberOfItems % 2 == 0) {
            slot = 4 - numberOfItems / 2;
        } else {
            slot = numberOfItems == 9 ? 0 : 0 + (9 - numberOfItems) / 2;
        }

        return slot;
    }
}
