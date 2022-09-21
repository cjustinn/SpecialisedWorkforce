package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import net.coreprotect.CoreProtectAPI;
import net.coreprotect.CoreProtectAPI.ParseResult;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.*;

public class WorkforceListener implements Listener {

    private Map<Location, BrewLog> brewingStandStatus = new HashMap<Location, BrewLog>();
    private Map<PotionEffectType, PotionDurationData> PotionDurationList = new HashMap<PotionEffectType, PotionDurationData>();

    // Initialise the values of the potion duration list.
    private void InitialiseDurationList() {
        if (this.PotionDurationList.size() == 0) {

            PotionDurationList.put(PotionEffectType.REGENERATION, new PotionDurationData(900, 440, 1800));
            PotionDurationList.put(PotionEffectType.SPEED, new PotionDurationData(3600, 1800, 9600));
            PotionDurationList.put(PotionEffectType.FIRE_RESISTANCE, new PotionDurationData(3600, 0, 9600));
            PotionDurationList.put(PotionEffectType.HEAL, new PotionDurationData(0, 0, 0));
            PotionDurationList.put(PotionEffectType.NIGHT_VISION, new PotionDurationData(3600, 0, 9600));
            PotionDurationList.put(PotionEffectType.INCREASE_DAMAGE, new PotionDurationData(3600, 1800, 9600));
            PotionDurationList.put(PotionEffectType.JUMP, new PotionDurationData(3600, 1800, 9600));
            PotionDurationList.put(PotionEffectType.WATER_BREATHING, new PotionDurationData(3600, 0, 9600));
            PotionDurationList.put(PotionEffectType.INVISIBILITY, new PotionDurationData(3600, 0, 9600));
            PotionDurationList.put(PotionEffectType.SLOW_FALLING, new PotionDurationData(3600, 0, 9600));
            PotionDurationList.put(PotionEffectType.LUCK, new PotionDurationData(6000, 0, 0));

        }
    }

    // Logs when a block is placed, used by the BONUS_BLOCK_DROPS listener.
    @EventHandler
    public void BlockPlaced(BlockPlaceEvent event) {
        BlockData _bd = event.getBlockPlaced().getBlockData();
        if (_bd instanceof Sapling || _bd instanceof Ageable) return;
        else {
            CoreProtectAPI cpAPI = LoggingManager.GetCoreProtect();
            if (cpAPI != null) {
                cpAPI.logPlacement(event.getPlayer().getName(), event.getBlockPlaced().getLocation(), event.getBlockPlaced().getType(), event.getBlockPlaced().getBlockData());
            }
        }
    }

    /*
        Listens for when blocks are broken, and checks if the player is employed, and
        if their job has a BONUS_BLOCK_DROPS attribute attached to it.
    */
    @EventHandler
    public void BlockDroppedItem(BlockDropItemEvent event) {
        final String plyUuid = event.getPlayer().getUniqueId().toString();

        ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            if (WorkforceManager.PlayerHasJobWithAttribute(plyUuid, JobAttributeType.BONUS_BLOCK_DROPS)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BONUS_BLOCK_DROPS);

                if (plyJob != null) {

                    // Check for block drop effects.
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BONUS_BLOCK_DROPS);

                    if (attr != null) {

                        // Check if the block broken matches attribute targets.
                        String blockTypeName = event.getBlockState().getType().toString().toLowerCase();
                        boolean blockMatch = attr.StringEndsWithTarget(blockTypeName);

                        if (blockMatch) {
                            // If the block is ageable and not at full growth, do nothing.
                            BlockData _bd = event.getBlockState().getBlockData();
                            if (_bd instanceof Ageable) {
                                if (((Ageable) _bd).getAge() < ((Ageable) _bd).getMaximumAge()) {
                                    return;
                                }
                            } else if (!(_bd instanceof Sapling)) {
                                CoreProtectAPI cpAPI = LoggingManager.GetCoreProtect();
                                if (cpAPI != null) {
                                    List<String[]> res = cpAPI.blockLookup(event.getBlockState().getBlock(), (31 * 24 * 60 * 60));
                                    if (res != null) {
                                        boolean natural = true;

                                        if (res.size() >= 1) {
                                            ParseResult pRes = cpAPI.parseResult(res.get(0));
                                            if (pRes.getActionId() == 1 && pRes.getType().equals(_bd.getMaterial()))
                                                natural = false;
                                        }

                                        if (!natural) return;
                                    }
                                }
                            }

                            // Pay the player, if payments are enabled for their job.
                            if (plyJob.getJob().getPaymentEnabled()) {
                                final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double paymentAmount = 0.0;
                                try {
                                    paymentAmount = new Double(sEngine.eval(paymentEq).toString());
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, paymentAmount);
                            }

                            // Give the user experience.
                            final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int expGain = 0;
                            try {
                                expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);

                            // Calculate chance of having drops increased.
                            final String dropIncChanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double dropIncChanceThreshold = -1.0;

                            try {
                                dropIncChanceThreshold = new Double(sEngine.eval(dropIncChanceEq).toString());
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            Random randomGen = new Random();
                            final double incChance = randomGen.nextDouble();

                            if (incChance <= dropIncChanceThreshold) {
                                final String amountIncEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));

                                // Calculate the amount of drops that should be added.
                                int dropIncAmt = 0;
                                try {
                                    dropIncAmt = (int) Math.floor(new Double(sEngine.eval(amountIncEq).toString()));
                                } catch(ScriptException e) {
                                    e.printStackTrace();
                                }

                                // Modify the getItems() list.
                                for (int i = 0; i < event.getItems().size(); i++) {
                                    ItemStack item = event.getItems().get(i).getItemStack();
                                    item.setAmount(item.getAmount() + dropIncAmt);

                                    event.getItems().get(i).setItemStack(item);
                                }
                            }
                        }

                    }

                }
            }
        }
    }

    /*
        Listens for when a player's tool takes durability damage,
        and is used to handle the DURABILITY_SAVE attribute.
     */
    @EventHandler
    public void PlayerItemDamaged(PlayerItemDamageEvent event) {
        final String plyUuid = event.getPlayer().getUniqueId().toString();

        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);

            if (WorkforceManager.PlayerHasJobWithAttribute(plyUuid, JobAttributeType.DURABILITY_SAVE)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.DURABILITY_SAVE);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.DURABILITY_SAVE);

                String itemType = event.getItem().getType().toString().toLowerCase();
                boolean toolMatch = attr.StringEndsWithTarget(itemType);

                if (toolMatch) {
                    // Calculate the chance of preventing durability damage.
                    final String chanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));

                    ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();
                    double chanceThreshold = -1.0;
                    try {
                        chanceThreshold = new Double(sEngine.eval(chanceEq).toString());
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }

                    // Roll to see if the durability will be saved.
                    double chance = 2.0;
                    Random numGen = new Random();

                    chance = numGen.nextDouble();

                    if (chance <= chanceThreshold) {
                        event.setCancelled(true);
                    }

                }
            }
        }
    }
    
    @EventHandler
    public void ItemWasCrafted(CraftItemEvent event) {
        final String plyUuid = event.getWhoClicked().getUniqueId().toString();

        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);

            if (ply.playerHasJobWithAttribute(JobAttributeType.CRAFTING_EXPERIENCE_GAIN)) {

                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.CRAFTING_EXPERIENCE_GAIN);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.CRAFTING_EXPERIENCE_GAIN);
                if (attr != null) {
                    final String craftedItemName = event.getRecipe().getResult().getType().toString().toLowerCase();
                    boolean itemMatch = attr.StringEndsWithTarget(craftedItemName);

                    if (itemMatch) {
                        // Item matches one of the target items, add experience to the player according to the equation.
                        ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                        // Pay the player, if payments are enabled for their job.
                        if (plyJob.getJob().getPaymentEnabled()) {
                            final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double paymentAmount = 0.0;
                            try {
                                paymentAmount = new Double(sEngine.eval(paymentEq).toString());
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            SpecialisedWorkforce.PayPlayer(plyUuid, paymentAmount);
                        }

                        // Give the user profession experience.
                        final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                        int expGain = 0;
                        try {
                            expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                        } catch (ScriptException e) {
                            e.printStackTrace();
                        }

                        WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);

                        // Give the user vanilla experience
                        final String vanillaExperienceEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        int expAmount = 0;

                        try {
                            expAmount = (int) Math.ceil(new Double(sEngine.eval(vanillaExperienceEq).toString()));
                        } catch(ScriptException e) {
                            e.printStackTrace();
                        }

                        ((Player) event.getWhoClicked()).giveExp(expAmount);
                    }
                }

            }
        }
    }

    /*
        Used to adjust anvil resource and experience costs by the
        ANVIL_COST_REDUCTION attribute.
     */
    @EventHandler
    public void AnvilIsUsed(PrepareAnvilEvent event) {
        String plyUuid = null;

        for (HumanEntity ply : event.getInventory().getViewers()) {
            if (WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString()) && WorkforceManager.PlayerHasJobWithAttribute(ply.getUniqueId().toString(), JobAttributeType.ANVIL_COST_REDUCTION)) {
                plyUuid = ply.getUniqueId().toString();
                break;
            }
        }

        if (plyUuid != null) {
            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ANVIL_COST_REDUCTION);

            if (plyJob != null) {

                // Get the attribute.
                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ANVIL_COST_REDUCTION);

                if (attr != null) {
                    // Get the value to multiply the anvil costs by.
                    ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();
                    final String modifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double modifier = 1.0;

                    try {
                        modifier = new Double(sEngine.eval(modifierEq).toString());
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }

                    // Get the reduced values.
                    int expCost = (int) Math.floor(event.getInventory().getRepairCost() * (1.0 - modifier));
                    int resourceCost = (int) Math.floor(event.getInventory().getRepairCostAmount() * (1.0 - modifier));

                    expCost = expCost < 1 ? 1 : expCost;
                    resourceCost = resourceCost < 1 ? 1 : resourceCost;

                    // Set the reduced values via scheduled task.
                    int finalResourceCost = resourceCost;
                    int finalExpCost = expCost;

                    Bukkit.getServer().getScheduler().runTask(Bukkit.getServer().getPluginManager().getPlugin("SpecialisedWorkforce"), () -> {
                        event.getInventory().setRepairCost(finalExpCost);
                        event.getInventory().setRepairCostAmount(finalResourceCost);
                    });
                }


            }
        }
    }

    /*
        Used to parse information about potentially profession-relevant
        inventory interactions.

        USED BY ATTRIBUTES:
        - ANVIL_COST_REDUCTION
        - POTION_DURATION_BOOST
     */
    @EventHandler
    public void InventoryWasClicked(InventoryClickEvent event) {
        final String plyUuid = event.getWhoClicked().getUniqueId().toString();
        PlayerJobData ply = null;
        PlayerJob plyJob = null;

        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            ply = WorkforceManager.GetPlayerData(plyUuid);
        } else return;

        if (ply != null) {
            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

            // ANVIL_COST_REDUCTION
            if (ply.playerHasJobWithAttribute(JobAttributeType.ANVIL_COST_REDUCTION) && event.getInventory() instanceof AnvilInventory) {
                if (event.getCurrentItem().getType().isAir() || event.getSlotType() != InventoryType.SlotType.RESULT) return;

                plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ANVIL_COST_REDUCTION);

                if (plyJob.getJob().getPaymentEnabled()) {
                    final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double amount = 0.0;

                    try {
                        amount = new Double(sEngine.eval(paymentEq).toString());
                    } catch(ScriptException e) {
                        e.printStackTrace();
                    }

                    SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                }

                // Give the user experience.
                final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                int expGain = 0;
                try {
                    expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                } catch (ScriptException e) {
                    e.printStackTrace();
                }

                WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);
            }
            else if (event.getInventory() instanceof BrewerInventory) {
                if (event.getCursor().getType().isAir() && !event.isShiftClick()) return;

                final Material[] ingredients = new Material[] {
                        Material.NETHER_WART,
                        Material.REDSTONE,
                        Material.GLOWSTONE_DUST,
                        Material.FERMENTED_SPIDER_EYE,
                        Material.GUNPOWDER,
                        Material.DRAGON_BREATH,
                        Material.SUGAR,
                        Material.RABBIT_FOOT,
                        Material.GLISTERING_MELON_SLICE,
                        Material.SPIDER_EYE,
                        Material.PUFFERFISH,
                        Material.MAGMA_CREAM,
                        Material.GOLDEN_CARROT,
                        Material.BLAZE_POWDER,
                        Material.GHAST_TEAR,
                        Material.TURTLE_HELMET,
                        Material.PHANTOM_MEMBRANE
                };
                boolean isIngredient = false;

                for (int i = 0; i < ingredients.length && !isIngredient; i++) {
                    if (ingredients[i].equals(event.getCurrentItem().getType()))
                        isIngredient = true;
                }

                if ((event.isShiftClick() && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && ((BrewerInventory) event.getView().getTopInventory()).getIngredient() == null && isIngredient) || event.getSlotType() == InventoryType.SlotType.FUEL) {
                    if (event.isShiftClick() && event.getCurrentItem().getType().equals(Material.BLAZE_POWDER)) {
                        if (((BrewerInventory) event.getView().getTopInventory()).getFuel() == null)
                            return;
                        else if (((BrewerInventory) event.getView().getTopInventory()).getFuel() != null && ((BrewerInventory) event.getView().getTopInventory()).getFuel().getAmount() != ((BrewerInventory) event.getView().getTopInventory()).getFuel().getType().getMaxStackSize())
                            return;
                    }

                    boolean plyJobHasAttribute = ply.playerHasJobWithAttribute(JobAttributeType.POTION_DURATION_BOOST);
                    if (plyJobHasAttribute)
                        plyJob = ply.getPlayerJobByAttribute(JobAttributeType.POTION_DURATION_BOOST);

                    if (brewingStandStatus.containsKey(event.getInventory().getLocation())) {
                        brewingStandStatus.replace(event.getInventory().getLocation(), new BrewLog(plyJobHasAttribute, plyJob.getLevel(), ply.getUuid()));
                    } else {
                        brewingStandStatus.put(event.getInventory().getLocation(), new BrewLog(plyJobHasAttribute, plyJob.getLevel(), ply.getUuid()));
                    }
                }
            }
        }
    }

    /*
        Used to get information about the player's
        fishing activities.

        Used by BONUS_FISHING_DROPS attribute.
     */
    @EventHandler
    public void PlayerFishing(PlayerFishEvent event) {
        if (event.getCaught() == null || event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY && event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        final String plyUuid = event.getPlayer().getUniqueId().toString();
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);

            if (ply.playerHasJobWithAttribute(JobAttributeType.BONUS_FISHING_DROPS)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BONUS_FISHING_DROPS);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BONUS_FISHING_DROPS);

                if (attr != null) {

                    if (event.getCaught().getType() == EntityType.DROPPED_ITEM) {
                        final String caughtItemType = ((Item) event.getCaught()).getItemStack().getType().toString();

                        boolean matchFound = attr.StringEndsWithTarget(caughtItemType);

                        if (matchFound) {

                            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                            // If payment is enabled, pay the user.
                            if (plyJob.getJob().getPaymentEnabled()) {
                                final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double amount = 0.0;

                                try {
                                    amount = new Double(sEngine.eval(paymentEq).toString());
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                            }

                            // Give the user profession experience.
                            final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int expGain = 0;
                            try {
                                expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);

                            // Calculate if the drop amount should be increased
                            final String chanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double chanceThreshold = -1.0;

                            try {
                                chanceThreshold = new Double(sEngine.eval(chanceEq).toString());
                            } catch(ScriptException e) {
                                e.printStackTrace();
                            }

                            Random numGen = new Random();
                            final double chance = numGen.nextDouble();

                            // If drops need increased, calculate by how much and apply the change.
                            if (chance <= chanceThreshold) {
                                final String incEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int incAmount = 0;

                                try {
                                    incAmount = (int) Math.floor(new Double(sEngine.eval(incEq).toString()));
                                } catch(ScriptException e) {
                                    e.printStackTrace();
                                }

                                ItemStack _item = ((Item) event.getCaught()).getItemStack();
                                _item.setAmount(_item.getAmount() + incAmount);
                                ((Item) event.getCaught()).setItemStack(_item);

                            }
                        }
                    }

                }
            }
        }
    }

    /*
        Runs whenever a mob / entity dies.

        Used by attribute(s):
        - BONUS_MOB_DROPS
     */
    @EventHandler
    public void MobDiedHandler(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer != null) {

            final String plyUuid = killer.getUniqueId().toString();
            if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

                final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);

                if (ply.playerHasJobWithAttribute(JobAttributeType.BONUS_MOB_DROPS)) {
                    final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BONUS_MOB_DROPS);

                    final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BONUS_MOB_DROPS);

                    if (attr != null) {

                        final String targetEntityName = event.getEntityType().toString();
                        boolean matchFound = attr.StringEndsWithTarget(targetEntityName);

                        if (matchFound) {

                            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                            // If payment is enabled, pay the user.
                            if (plyJob.getJob().getPaymentEnabled()) {
                                final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double amount = 0.0;

                                try {
                                    amount = new Double(sEngine.eval(paymentEq).toString());
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                            }

                            // Give the user profession experience.
                            final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int expGain = 0;
                            try {
                                expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);

                            // Calculate the chance of having increased drops.
                            final String chanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double chanceThreshold = -1.0;

                            try {
                                chanceThreshold = new Double(sEngine.eval(chanceEq).toString());
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            Random numGen = new Random();
                            final double chance = numGen.nextDouble();

                            // If drops should be increased, calculate and add the extra drops.
                            if (chance <= chanceThreshold) {
                                final String amountEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int amount = 0;

                                try {
                                    amount = (int) Math.floor(new Double(sEngine.eval(amountEq).toString()));
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                for (int i = 0; i < event.getDrops().size(); i++) {
                                    event.getDrops().get(i).setAmount(event.getDrops().get(i).getAmount() + amount);
                                }
                            }

                        }

                    }
                }
            }
        }
    }

    private boolean StringStartsWithVowel(String target) {
        boolean startsWithVowel = false;
        
        char beginning = target.toLowerCase().charAt(0);
        final char[] vowels = new char[] { 'a', 'e', 'i', 'o', 'u' };
        
        for (char c : vowels) {
            if (beginning == c)
                startsWithVowel = true;
        }
        
        return startsWithVowel;
    }

    @EventHandler
    public void BrewingCompleted(BrewEvent event) {
        Location standLocation = event.getContents().getLocation();

        if (brewingStandStatus.containsKey(standLocation)) {
            BrewLog standLog = brewingStandStatus.get(standLocation);

            if (standLog.brewerIsAlchemist()) {
                final PlayerJobData ply = WorkforceManager.GetPlayerData(standLog.getBrewerUuid());

                if (ply.playerHasJobWithAttribute(JobAttributeType.POTION_DURATION_BOOST)) {
                    final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.POTION_DURATION_BOOST);

                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.POTION_DURATION_BOOST);

                    ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                    // Loop through all brewed potions, track how many are valid potions and increase duration of any which are.
                    InitialiseDurationList();

                    int validPotions = 0;
                    for (int i = 0; i < event.getResults().size(); i++) {
                        if (event.getResults().get(i).getType() == Material.POTION || event.getResults().get(i).getType() == Material.LINGERING_POTION || event.getResults().get(i).getType() == Material.SPLASH_POTION) {
                            PotionType _type = ((PotionMeta) event.getResults().get(i).getItemMeta()).getBasePotionData().getType();

                            if (_type != PotionType.AWKWARD && _type != PotionType.MUNDANE && _type != PotionType.UNCRAFTABLE && _type != PotionType.WATER) {
                                validPotions++;

                                final String incEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int tickIncreaseAmt = 0;

                                try {
                                    tickIncreaseAmt = (int) Math.floor(new Double(sEngine.eval(incEq).toString()));
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                PotionEffectType _effType = _type.getEffectType();
                                boolean isUpgraded = ((PotionMeta) event.getResults().get(i).getItemMeta()).getBasePotionData().isUpgraded();
                                boolean isExtended = ((PotionMeta) event.getResults().get(i).getItemMeta()).getBasePotionData().isExtended();

                                final int defaultDuration = isUpgraded ? PotionDurationList.get(_effType).GetUpgradedDuration() : (isExtended ? PotionDurationList.get(_effType).GetExtendedDuration() : PotionDurationList.get(_effType).GetBaseDuration());
                                final int duration = defaultDuration != 0 ? defaultDuration + tickIncreaseAmt : defaultDuration;

                                PotionEffect _overrideEffect = new PotionEffect(
                                        _effType,
                                        duration,
                                        isUpgraded ? 1 : 0
                                );

                                PotionMeta _pMeta = (PotionMeta) event.getResults().get(i).getItemMeta();
                                _pMeta.addCustomEffect(_overrideEffect, true);

                                List<String> lore = new ArrayList<String>();
                                lore.add("");
                                lore.add(String.format("%sThis potion was brewed by", ChatColor.GRAY));
                                lore.add(String.format("%sa%s %s%s%s.", ChatColor.GRAY, StringStartsWithVowel(plyJob.getJob().getName()) ? "n" : "", ChatColor.GOLD, plyJob.getJob().getName(), ChatColor.RESET));

                                lore.add("");
                                if (duration > 0)
                                    lore.add(String.format("%sDuration: %s%d:%02d", ChatColor.GRAY, ChatColor.GOLD, ((duration / 20) / 60), ((duration / 20) % 60)));
                                else
                                    lore.add(String.format("%sInstant Potion%s", ChatColor.GOLD, ChatColor.RESET));
                                lore.add("");

                                _pMeta.setLore(lore);

                                event.getResults().get(i).setItemMeta(_pMeta);
                            }
                        }
                    }

                    // Pay the user, if it's enabled.
                    if (plyJob.getJob().getPaymentEnabled()) {
                        final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        double paymentAmount = 0.0;

                        try {
                            paymentAmount = new Double(sEngine.eval(paymentEq).toString()) * (validPotions / 3.0);
                        } catch (ScriptException e) {
                            e.printStackTrace();
                        }

                        SpecialisedWorkforce.PayPlayer(ply.getUuid(), paymentAmount);
                    }

                    // Give the user profession experience.
                    final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                    int expGain = 0;
                    try {
                        expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()) * (validPotions / 3.0));
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }

                    WorkforceManager.AddExperiencePointsToPlayer(ply.getUuid(), plyJob.getJob().getName(), expGain);
                }

            }
        }
    }

    /*
        Used to get information on enchanting activities.

        USED BY:
        - ENCHANTING_LEVEL_BOOST
        - ENCHANTING_COST_REDUCTION
    */
    @EventHandler
    public void PlayerIsEnchanting(PrepareItemEnchantEvent event) {

        final String plyUuid = event.getEnchanter().getUniqueId().toString();

        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);

            // Enchanting level boost.
            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_LEVEL_BOOST);

                if (attr != null) {
                    ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                    // Get the number of levels to increase the enchantment by.
                    final String incEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    int amount = 0;

                    try {
                        amount = (int) Math.floor(new Double(sEngine.eval(incEq).toString()));
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }

                    // Iterate through each enchantment, if not null then
                    // increase the level of that enchantment, capping it
                    // out at maxLevel + 1.

                    int finalAmount = amount;

                    for (int i = 0; i < event.getOffers().length; i++) {
                        if (event.getOffers()[i] != null) {
                            int lvl = event.getOffers()[i].getEnchantmentLevel() + finalAmount;
                            event.getOffers()[i].setEnchantmentLevel(lvl > (event.getOffers()[i].getEnchantment().getMaxLevel() + 1) ? (event.getOffers()[i].getEnchantment().getMaxLevel() + 1) : lvl);
                        }
                    }
                }
            }
            // Enchanting cost reduction.
            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_COST_REDUCTION);

                if (attr != null) {

                    ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                    // Get level modifier
                    final String modifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double modifier = 0.0;

                    try {
                        modifier = Double.parseDouble(sEngine.eval(modifierEq).toString());
                    } catch (ScriptException e) { e.printStackTrace(); }

                    final double modifierVal = 1.0 - modifier;

                    for (int i = 0; i < event.getOffers().length; i++) {
                        if (event.getOffers()[i] != null) {
                            event.getOffers()[i].setCost((int) Math.ceil(event.getOffers()[i].getCost() * modifierVal));
                        }
                    }

                }
            }

        }

    }

    @EventHandler
    public void ItemEnchantedHandler(EnchantItemEvent event) {
        final String plyUuid = event.getEnchanter().getUniqueId().toString();

        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            final PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);

            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_COST_REDUCTION);

                if (attr != null) {
                    final String costModifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double modifier = 0.0;

                    try {
                        modifier = Double.parseDouble(sEngine.eval(costModifierEq).toString());
                    } catch (ScriptException e) { e.printStackTrace(); }

                    event.setExpLevelCost((int) Math.ceil(event.getExpLevelCost() * (1.0 - modifier)));

                    // Pay the user if the job has payments enabled.
                    if (plyJob.getJob().getPaymentEnabled()) {
                        final String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        double amount = 0.0;

                        try {
                            amount = new Double(sEngine.eval(paymentEq).toString());
                        } catch (ScriptException e) { e.printStackTrace(); }

                        SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                    }

                    // Give the user profession experience.
                    final String experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                    int expGain = 0;
                    try {
                        expGain = (int) Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }

                    WorkforceManager.AddExperiencePointsToPlayer(ply.getUuid(), plyJob.getJob().getName(), expGain);
                }
            }
            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST)) {
                final PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST);

                final JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_LEVEL_BOOST);

                final String levelModifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                int amount = 0;

                try {
                    amount = (int) Math.ceil(Double.parseDouble(sEngine.eval(levelModifierEq).toString()));
                } catch (ScriptException e) { e.printStackTrace(); }

                Set<Enchantment> enchantmentKeys = event.getEnchantsToAdd().keySet();
                for (Enchantment _enc : enchantmentKeys) {
                    final int combinedAmount = event.getEnchantsToAdd().get(_enc) + amount;
                    event.getEnchantsToAdd().replace(_enc, combinedAmount > (_enc.getMaxLevel() + 1) ? (_enc.getMaxLevel() + 1) : combinedAmount);
                }
            }
        }
    }
}
