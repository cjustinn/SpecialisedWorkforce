package io.github.cjustinn.specialisedworkforce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class WorkforceListener implements Listener {
    private Map<Location, BrewLog> brewingStandStatus = new HashMap();
    private Map<PotionEffectType, PotionDurationData> PotionDurationList = new HashMap();
    private Map<Material, Integer> BaseScoreChart = new HashMap();
    private Map<Enchantment, Integer> EnchantmentScoreChart = new HashMap();

    public WorkforceListener() {
    }

    private void InitialiseDurationList() {
        if (this.PotionDurationList.size() == 0) {
            this.PotionDurationList.put(PotionEffectType.REGENERATION, new PotionDurationData(900, 440, 1800));
            this.PotionDurationList.put(PotionEffectType.SPEED, new PotionDurationData(3600, 1800, 9600));
            this.PotionDurationList.put(PotionEffectType.FIRE_RESISTANCE, new PotionDurationData(3600, 0, 9600));
            this.PotionDurationList.put(PotionEffectType.HEAL, new PotionDurationData(0, 0, 0));
            this.PotionDurationList.put(PotionEffectType.NIGHT_VISION, new PotionDurationData(3600, 0, 9600));
            this.PotionDurationList.put(PotionEffectType.INCREASE_DAMAGE, new PotionDurationData(3600, 1800, 9600));
            this.PotionDurationList.put(PotionEffectType.JUMP, new PotionDurationData(3600, 1800, 9600));
            this.PotionDurationList.put(PotionEffectType.WATER_BREATHING, new PotionDurationData(3600, 0, 9600));
            this.PotionDurationList.put(PotionEffectType.INVISIBILITY, new PotionDurationData(3600, 0, 9600));
            this.PotionDurationList.put(PotionEffectType.SLOW_FALLING, new PotionDurationData(3600, 0, 9600));
            this.PotionDurationList.put(PotionEffectType.LUCK, new PotionDurationData(6000, 0, 0));
        }

    }

    @EventHandler
    public void BlockPlaced(BlockPlaceEvent event) {
        BlockData _bd = event.getBlockPlaced().getBlockData();
        if (!(_bd instanceof Sapling) && !(_bd instanceof Ageable)) {
            CoreProtectAPI cpAPI = LoggingManager.GetCoreProtect();
            if (cpAPI != null) {
                cpAPI.logPlacement(event.getPlayer().getName(), event.getBlockPlaced().getLocation(), event.getBlockPlaced().getType(), event.getBlockPlaced().getBlockData());
            }

        }
    }

    // Logs when a block is removed.
    @EventHandler
    public void BlockBroken(BlockBreakEvent event) {
        BlockData _bd = event.getBlock().getBlockData();
        if (_bd instanceof Sapling || _bd instanceof Ageable) return;
        else {
            CoreProtectAPI cpAPI = LoggingManager.GetCoreProtect();
            if (cpAPI != null) {
                cpAPI.logRemoval(event.getPlayer().getName(), event.getBlock().getLocation(), event.getBlock().getType(), event.getBlock().getBlockData());
            }
        }
    }

    @EventHandler
    public void BlockDroppedItem(BlockDropItemEvent event) {
        String plyUuid = event.getPlayer().getUniqueId().toString();
        ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            if (WorkforceManager.PlayerHasJobWithAttribute(plyUuid, JobAttributeType.BONUS_BLOCK_DROPS)) {
                PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BONUS_BLOCK_DROPS);
                if (plyJob != null) {
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BONUS_BLOCK_DROPS);
                    if (attr != null) {
                        String blockTypeName = event.getBlockState().getType().toString().toLowerCase();

                        boolean blockMatch = attr.StringEndsWithTarget(blockTypeName);
                        if (blockMatch) {
                            BlockData _bd = event.getBlockState().getBlockData();
                            if (_bd instanceof Ageable) {
                                if (((Ageable)_bd).getAge() < ((Ageable)_bd).getMaximumAge()) {
                                    return;
                                }
                            } else if (!(_bd instanceof Sapling)) {
                                CoreProtectAPI cpAPI = LoggingManager.GetCoreProtect();
                                if (cpAPI != null) {
                                    List<String[]> res = cpAPI.blockLookup(event.getBlockState().getBlock(), 2678400);
                                    if (res != null) {
                                        boolean natural = true;
                                        if (res.size() >= 1) {
                                            CoreProtectAPI.ParseResult pRes = cpAPI.parseResult((String[])res.get(0));
                                            if (pRes.getActionId() == 1 && pRes.getType().equals(_bd.getMaterial())) {
                                                natural = false;
                                            }
                                        }

                                        if (!natural) {
                                            return;
                                        }
                                    }
                                }
                            }

                            String experienceEq;
                            if (plyJob.getJob().getPaymentEnabled()) {
                                experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double paymentAmount = 0.0;

                                try {
                                    paymentAmount = new Double(sEngine.eval(experienceEq).toString());
                                } catch (ScriptException var25) {
                                    var25.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, paymentAmount);
                            }

                            experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int expGain = 0;

                            try {
                                expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                            } catch (ScriptException var24) {
                                var24.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);
                            String dropIncChanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double dropIncChanceThreshold = -1.0;

                            try {
                                dropIncChanceThreshold = new Double(sEngine.eval(dropIncChanceEq).toString());
                            } catch (ScriptException var23) {
                                var23.printStackTrace();
                            }

                            Random randomGen = new Random();
                            double incChance = randomGen.nextDouble();
                            if (incChance <= dropIncChanceThreshold) {
                                String amountIncEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int dropIncAmt = 0;

                                try {
                                    dropIncAmt = (int)Math.floor(new Double(sEngine.eval(amountIncEq).toString()));
                                } catch (ScriptException var22) {
                                    var22.printStackTrace();
                                }

                                for(int i = 0; i < event.getItems().size(); ++i) {
                                    ItemStack item = ((Item)event.getItems().get(i)).getItemStack();
                                    item.setAmount(item.getAmount() + dropIncAmt);
                                    ((Item)event.getItems().get(i)).setItemStack(item);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @EventHandler
    public void PlayerItemDamaged(PlayerItemDamageEvent event) {
        String plyUuid = event.getPlayer().getUniqueId().toString();
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            if (WorkforceManager.PlayerHasJobWithAttribute(plyUuid, JobAttributeType.DURABILITY_SAVE)) {
                PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.DURABILITY_SAVE);
                JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.DURABILITY_SAVE);
                String itemType = event.getItem().getType().toString().toLowerCase();
                boolean toolMatch = attr.StringEndsWithTarget(itemType);
                if (toolMatch) {
                    String chanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                    ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                    double chanceThreshold = -1.0;

                    try {
                        chanceThreshold = new Double(sEngine.eval(chanceEq).toString());
                    } catch (ScriptException var15) {
                        var15.printStackTrace();
                    }

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
        String plyUuid = event.getWhoClicked().getUniqueId().toString();
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            if (ply.playerHasJobWithAttribute(JobAttributeType.CRAFTING_EXPERIENCE_GAIN)) {
                PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.CRAFTING_EXPERIENCE_GAIN);
                JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.CRAFTING_EXPERIENCE_GAIN);
                if (attr != null) {
                    String craftedItemName = event.getRecipe().getResult().getType().toString().toLowerCase();
                    boolean itemMatch = attr.StringEndsWithTarget(craftedItemName);
                    if (itemMatch) {
                        ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                        String experienceEq;
                        if (plyJob.getJob().getPaymentEnabled()) {
                            experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double paymentAmount = 0.0;

                            try {
                                paymentAmount = new Double(sEngine.eval(experienceEq).toString());
                            } catch (ScriptException var16) {
                                var16.printStackTrace();
                            }

                            SpecialisedWorkforce.PayPlayer(plyUuid, paymentAmount);
                        }

                        experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                        int expGain = 0;

                        try {
                            expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                        } catch (ScriptException var15) {
                            var15.printStackTrace();
                        }

                        WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);
                        String vanillaExperienceEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        int expAmount = 0;

                        try {
                            expAmount = (int)Math.ceil(new Double(sEngine.eval(vanillaExperienceEq).toString()));
                        } catch (ScriptException var14) {
                            var14.printStackTrace();
                        }

                        ((Player)event.getWhoClicked()).giveExp(expAmount);
                    }
                }
            }
        }

    }

    @EventHandler
    public void AnvilIsUsed(PrepareAnvilEvent event) {
        String plyUuid = null;
        Iterator var3 = event.getInventory().getViewers().iterator();

        while(var3.hasNext()) {
            HumanEntity ply = (HumanEntity)var3.next();
            if (WorkforceManager.PlayerIsEmployed(ply.getUniqueId().toString()) && WorkforceManager.PlayerHasJobWithAttribute(ply.getUniqueId().toString(), JobAttributeType.ANVIL_COST_REDUCTION)) {
                plyUuid = ply.getUniqueId().toString();
                break;
            }
        }

        if (plyUuid != null) {
            PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ANVIL_COST_REDUCTION);
            if (plyJob != null) {
                JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ANVIL_COST_REDUCTION);
                if (attr != null) {
                    ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                    String modifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double modifier = 1.0;

                    try {
                        modifier = new Double(sEngine.eval(modifierEq).toString());
                    } catch (ScriptException var14) {
                        var14.printStackTrace();
                    }

                    int expCost = (int)Math.floor((double)event.getInventory().getRepairCost() * (1.0 - modifier));
                    int resourceCost = (int)Math.floor((double)event.getInventory().getRepairCostAmount() * (1.0 - modifier));
                    expCost = expCost < 1 ? 1 : expCost;
                    resourceCost = resourceCost < 1 ? 1 : resourceCost;

                    int finalExpCost = expCost;
                    int finalResourceCost = resourceCost;
                    Bukkit.getServer().getScheduler().runTask(Bukkit.getServer().getPluginManager().getPlugin("SpecialisedWorkforce"), () -> {
                        event.getInventory().setRepairCost(finalExpCost);
                        event.getInventory().setRepairCostAmount(finalResourceCost);
                    });
                }
            }
        }

    }

    @EventHandler
    public void InventoryWasClicked(InventoryClickEvent event) {
        String plyUuid = event.getWhoClicked().getUniqueId().toString();
        PlayerJobData ply = null;
        PlayerJob plyJob = null;
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            ply = WorkforceManager.GetPlayerData(plyUuid);
            if (ply != null) {
                ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                if (ply.playerHasJobWithAttribute(JobAttributeType.ANVIL_COST_REDUCTION) && event.getInventory() instanceof AnvilInventory) {
                    if (event.getCurrentItem().getType().isAir() || event.getSlotType() != SlotType.RESULT) {
                        return;
                    }

                    plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ANVIL_COST_REDUCTION);
                    String experienceEq;
                    if (plyJob.getJob().getPaymentEnabled()) {
                        experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        double amount = 0.0;

                        try {
                            amount = new Double(sEngine.eval(experienceEq).toString());
                        } catch (ScriptException var11) {
                            var11.printStackTrace();
                        }

                        SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                    }

                    experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                    int expGain = 0;

                    try {
                        expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                    } catch (ScriptException var10) {
                        var10.printStackTrace();
                    }

                    WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);
                } else if (event.getInventory() instanceof BrewerInventory) {
                    if (event.getCursor().getType().isAir() && !event.isShiftClick()) {
                        return;
                    }

                    Material[] ingredients = new Material[]{Material.NETHER_WART, Material.REDSTONE, Material.GLOWSTONE_DUST, Material.FERMENTED_SPIDER_EYE, Material.GUNPOWDER, Material.DRAGON_BREATH, Material.SUGAR, Material.RABBIT_FOOT, Material.GLISTERING_MELON_SLICE, Material.SPIDER_EYE, Material.PUFFERFISH, Material.MAGMA_CREAM, Material.GOLDEN_CARROT, Material.BLAZE_POWDER, Material.GHAST_TEAR, Material.TURTLE_HELMET, Material.PHANTOM_MEMBRANE};
                    boolean isIngredient = false;

                    for(int i = 0; i < ingredients.length && !isIngredient; ++i) {
                        if (ingredients[i].equals(event.getCurrentItem().getType())) {
                            isIngredient = true;
                        }
                    }

                    if (event.isShiftClick() && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && ((BrewerInventory)event.getView().getTopInventory()).getIngredient() == null && isIngredient || event.getSlotType() == SlotType.FUEL) {
                        if (event.isShiftClick() && event.getCurrentItem().getType().equals(Material.BLAZE_POWDER)) {
                            if (((BrewerInventory)event.getView().getTopInventory()).getFuel() == null) {
                                return;
                            }

                            if (((BrewerInventory)event.getView().getTopInventory()).getFuel() != null && ((BrewerInventory)event.getView().getTopInventory()).getFuel().getAmount() != ((BrewerInventory)event.getView().getTopInventory()).getFuel().getType().getMaxStackSize()) {
                                return;
                            }
                        }

                        boolean plyJobHasAttribute = ply.playerHasJobWithAttribute(JobAttributeType.POTION_DURATION_BOOST);
                        if (plyJobHasAttribute) {
                            plyJob = ply.getPlayerJobByAttribute(JobAttributeType.POTION_DURATION_BOOST);
                        }

                        if (this.brewingStandStatus.containsKey(event.getInventory().getLocation())) {
                            this.brewingStandStatus.replace(event.getInventory().getLocation(), new BrewLog(plyJobHasAttribute, plyJob.getLevel(), ply.getUuid()));
                        } else {
                            this.brewingStandStatus.put(event.getInventory().getLocation(), new BrewLog(plyJobHasAttribute, plyJob.getLevel(), ply.getUuid()));
                        }
                    }
                }
            }

        }
    }

    @EventHandler
    public void PlayerFishing(PlayerFishEvent event) {
        if (event.getCaught() != null && (event.getState() == State.CAUGHT_ENTITY || event.getState() == State.CAUGHT_FISH)) {
            String plyUuid = event.getPlayer().getUniqueId().toString();
            if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
                PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
                if (ply.playerHasJobWithAttribute(JobAttributeType.BONUS_FISHING_DROPS)) {
                    PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BONUS_FISHING_DROPS);
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BONUS_FISHING_DROPS);
                    if (attr != null && event.getCaught().getType() == EntityType.DROPPED_ITEM) {
                        String caughtItemType = ((Item)event.getCaught()).getItemStack().getType().toString();
                        boolean matchFound = attr.StringEndsWithTarget(caughtItemType);
                        if (matchFound) {
                            ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                            String experienceEq;
                            if (plyJob.getJob().getPaymentEnabled()) {
                                experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double amount = 0.0;

                                try {
                                    amount = new Double(sEngine.eval(experienceEq).toString());
                                } catch (ScriptException var23) {
                                    var23.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                            }

                            experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int expGain = 0;

                            try {
                                expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                            } catch (ScriptException var22) {
                                var22.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);
                            String chanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double chanceThreshold = -1.0;

                            try {
                                chanceThreshold = new Double(sEngine.eval(chanceEq).toString());
                            } catch (ScriptException var21) {
                                var21.printStackTrace();
                            }

                            Random numGen = new Random();
                            double chance = numGen.nextDouble();
                            if (chance <= chanceThreshold) {
                                String incEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int incAmount = 0;

                                try {
                                    incAmount = (int)Math.floor(new Double(sEngine.eval(incEq).toString()));
                                } catch (ScriptException var20) {
                                    var20.printStackTrace();
                                }

                                ItemStack _item = ((Item)event.getCaught()).getItemStack();
                                _item.setAmount(_item.getAmount() + incAmount);
                                ((Item)event.getCaught()).setItemStack(_item);
                            }
                        }
                    }
                }
            }

        }
    }

    @EventHandler
    public void PlayerInteracted(PlayerInteractEvent event) {
        Player _p = event.getPlayer();

        if (_p != null) {

            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

                String plyUuid = _p.getUniqueId().toString();
                if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

                    // Beekeeper attribute
                    PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
                    if (ply.playerHasJobWithAttribute(JobAttributeType.BEEKEEPER_BONUS)) {

                        // Check if the block was a beehive or a bee's nest.
                        if (event.getClickedBlock().getType().equals(Material.BEE_NEST) || event.getClickedBlock().getType().equals(Material.BEEHIVE)) {

                            // Check if the beehive is holding its max level of honey.
                            Block _block = event.getClickedBlock();
                            Beehive _hiveData = (Beehive) _block.getBlockData();

                            if (_hiveData.getHoneyLevel() >= _hiveData.getMaximumHoneyLevel()) {

                                ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();
                                PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BEEKEEPER_BONUS);
                                JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BEEKEEPER_BONUS);

                                // Pay the player
                                if (plyJob.getJob().getPaymentEnabled() == true) {
                                    String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                    double amount = 0.0;

                                    try {
                                        amount = new Double(sEngine.eval(paymentEq).toString());
                                    } catch (ScriptException e) {
                                        e.printStackTrace();
                                    }

                                    SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                                }

                                // Give the player job experience.
                                String jobExpEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                                int jobExpAmount = 0;

                                try {
                                    jobExpAmount = (int) Math.floor(new Double(sEngine.eval(jobExpEq).toString()));
                                } catch (ScriptException e) { e.printStackTrace(); }

                                WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), jobExpAmount);

                                // Give the player vanilla experience.
                                String vanillaExpEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int vanillaExpAmount = 0;

                                try {
                                    vanillaExpAmount = (int) Math.floor(new Double(sEngine.eval(vanillaExpEq).toString()));
                                } catch (ScriptException e) { e.printStackTrace(); }

                                _p.giveExp(vanillaExpAmount);

                            }

                        }

                    }

                }

            }

        }
    }

    @EventHandler
    public void EntityWasInteractedWith(PlayerInteractEntityEvent event) {
        Player _p = event.getPlayer();

        if (_p != null) {

            String plyUuid = _p.getUniqueId().toString();

            if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

                PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
                if (ply.playerHasJobWithAttribute(JobAttributeType.ENTITY_MILK) && event.getRightClicked().getType().equals(EntityType.COW)) {
                    PlayerInventory plyInv = _p.getInventory();
                    
                    if (plyInv.getItem(event.getHand()).getType().equals(Material.BUCKET)) {

                        ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                        PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENTITY_MILK);

                        // Pay player
                        if (plyJob.getJob().getPaymentEnabled() == true) {
                            String payEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double amount = 0.0;

                            try {
                                amount = new Double(sEngine.eval(payEq).toString());
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                        }

                        // Give job experience
                        String jobExpEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                        int expAmount = 0;

                        try {
                            expAmount = (int) Math.floor(new Double(sEngine.eval(jobExpEq).toString()));
                        } catch (ScriptException e) {
                            e.printStackTrace();
                        }

                        WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expAmount);
                    }
                }

            }

        }
    }

    @EventHandler
    public void EntitySheared(PlayerShearEntityEvent event) {
        Player _p = event.getPlayer();

        if (_p != null) {
            String plyUuid = _p.getUniqueId().toString();

            if (WorkforceManager.PlayerIsEmployed(plyUuid)) {

                PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
                if (ply.playerHasJobWithAttribute(JobAttributeType.ENTITY_SHEAR)) {

                    PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENTITY_SHEAR);
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENTITY_SHEAR);

                    if (attr != null) {

                        String animal = event.getEntity().getType().toString();
                        boolean matchFound = attr.StringEndsWithTarget(animal);

                        if (matchFound) {
                            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                            // Pay the player
                            if (plyJob.getJob().getPaymentEnabled() == true) {
                                String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double amount = 0.0;

                                try {
                                    amount = new Double(sEngine.eval(paymentEq).toString());
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                            }

                            // Give the player job experience
                            String jobExpEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int jobExpAmount = 0;

                            try {
                                jobExpAmount = (int) Math.floor(new Double(sEngine.eval(jobExpEq).toString()));
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), jobExpAmount);
                        }

                    }

                }

            }
        }
    }

    @EventHandler
    public void AnimalBred(EntityBreedEvent event) {
        Player breeder = (Player) event.getBreeder();

        if (breeder != null) {
            String plyUuid = breeder.getUniqueId().toString();

            if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
                PlayerJobData _ply = WorkforceManager.GetPlayerData(plyUuid);
                if (_ply.playerHasJobWithAttribute(JobAttributeType.BREEDING_BOOST)) {

                    PlayerJob plyJob = _ply.getPlayerJobByAttribute(JobAttributeType.BREEDING_BOOST);
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BREEDING_BOOST);

                    if (attr != null) {
                        String animalName = event.getEntity().getType().toString();
                        boolean matchFound = attr.StringEndsWithTarget(animalName);

                        if (matchFound) {
                            ScriptEngine sEngine = new NashornScriptEngineFactory().getScriptEngine();

                            // Pay the player
                            if (plyJob.getJob().getPaymentEnabled()) {
                                String paymentEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double amount = 0.0;

                                try {
                                    amount = new Double(sEngine.eval(paymentEq).toString());
                                } catch (ScriptException e) {
                                    e.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                            }

                            // Give the player job experience
                            String jobExpEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int jobExpGain = 0;

                            try {
                                jobExpGain = (int) Math.floor(new Double(sEngine.eval(jobExpEq).toString()));
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), jobExpGain);

                            // Calculate (based off the 'equation' field of the attribute) the amount of vanilla experience to add to the event result.
                            String vanillaExpEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                            int vanillaExpAmount = 0;

                            try {
                                vanillaExpAmount = (int) Math.floor(new Double(sEngine.eval(vanillaExpEq).toString()));
                            } catch (ScriptException e) {
                                e.printStackTrace();
                            }

                            event.setExperience(event.getExperience() + vanillaExpAmount);
                        }
                    }

                }
            }
        }
    }

    @EventHandler
    public void MobDiedHandler(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            String plyUuid = killer.getUniqueId().toString();
            if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
                PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
                if (ply.playerHasJobWithAttribute(JobAttributeType.BONUS_MOB_DROPS)) {
                    PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.BONUS_MOB_DROPS);
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.BONUS_MOB_DROPS);
                    if (attr != null) {
                        String targetEntityName = event.getEntityType().toString();
                        boolean matchFound = attr.StringEndsWithTarget(targetEntityName);
                        if (matchFound) {
                            ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                            String experienceEq;
                            if (plyJob.getJob().getPaymentEnabled()) {
                                experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                double amount = 0.0;

                                try {
                                    amount = new Double(sEngine.eval(experienceEq).toString());
                                } catch (ScriptException var24) {
                                    var24.printStackTrace();
                                }

                                SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                            }

                            experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                            int expGain = 0;

                            try {
                                expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                            } catch (ScriptException var23) {
                                var23.printStackTrace();
                            }

                            WorkforceManager.AddExperiencePointsToPlayer(plyUuid, plyJob.getJob().getName(), expGain);
                            String chanceEq = attr.getChance().replace("{level}", String.format("%d", plyJob.getLevel()));
                            double chanceThreshold = -1.0;

                            try {
                                chanceThreshold = new Double(sEngine.eval(chanceEq).toString());
                            } catch (ScriptException var22) {
                                var22.printStackTrace();
                            }

                            Random numGen = new Random();
                            double chance = numGen.nextDouble();
                            if (chance <= chanceThreshold) {
                                String amountEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int amount = 0;

                                try {
                                    amount = (int)Math.floor(new Double(sEngine.eval(amountEq).toString()));
                                } catch (ScriptException var21) {
                                    var21.printStackTrace();
                                }

                                for(int i = 0; i < event.getDrops().size(); ++i) {
                                    ((ItemStack)event.getDrops().get(i)).setAmount(((ItemStack)event.getDrops().get(i)).getAmount() + amount);
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

    @EventHandler
    public void BrewingCompleted(BrewEvent event) {
        Location standLocation = event.getContents().getLocation();
        if (this.brewingStandStatus.containsKey(standLocation)) {
            BrewLog standLog = (BrewLog)this.brewingStandStatus.get(standLocation);
            if (standLog.brewerIsAlchemist()) {
                PlayerJobData ply = WorkforceManager.GetPlayerData(standLog.getBrewerUuid());
                if (ply.playerHasJobWithAttribute(JobAttributeType.POTION_DURATION_BOOST)) {
                    PlayerJob plyJob = ply.getPlayerJobByAttribute(JobAttributeType.POTION_DURATION_BOOST);
                    JobAttribute attr = plyJob.getJob().getAttributeByType(JobAttributeType.POTION_DURATION_BOOST);
                    ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                    this.InitialiseDurationList();
                    int validPotions = 0;

                    for(int i = 0; i < event.getResults().size(); ++i) {
                        if (((ItemStack)event.getResults().get(i)).getType() == Material.POTION || ((ItemStack)event.getResults().get(i)).getType() == Material.LINGERING_POTION || ((ItemStack)event.getResults().get(i)).getType() == Material.SPLASH_POTION) {
                            PotionType _type = ((PotionMeta)((ItemStack)event.getResults().get(i)).getItemMeta()).getBasePotionData().getType();
                            if (_type != PotionType.AWKWARD && _type != PotionType.MUNDANE && _type != PotionType.UNCRAFTABLE && _type != PotionType.WATER) {
                                ++validPotions;
                                String incEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                                int tickIncreaseAmt = 0;

                                try {
                                    tickIncreaseAmt = (int)Math.floor(new Double(sEngine.eval(incEq).toString()));
                                } catch (ScriptException var23) {
                                    var23.printStackTrace();
                                }

                                PotionEffectType _effType = _type.getEffectType();
                                boolean isUpgraded = ((PotionMeta)((ItemStack)event.getResults().get(i)).getItemMeta()).getBasePotionData().isUpgraded();
                                boolean isExtended = ((PotionMeta)((ItemStack)event.getResults().get(i)).getItemMeta()).getBasePotionData().isExtended();
                                int defaultDuration = isUpgraded ? ((PotionDurationData)this.PotionDurationList.get(_effType)).GetUpgradedDuration() : (isExtended ? ((PotionDurationData)this.PotionDurationList.get(_effType)).GetExtendedDuration() : ((PotionDurationData)this.PotionDurationList.get(_effType)).GetBaseDuration());
                                int duration = defaultDuration != 0 ? defaultDuration + tickIncreaseAmt : defaultDuration;
                                PotionEffect _overrideEffect = new PotionEffect(_effType, duration, isUpgraded ? 1 : 0);
                                PotionMeta _pMeta = (PotionMeta)((ItemStack)event.getResults().get(i)).getItemMeta();
                                _pMeta.addCustomEffect(_overrideEffect, true);
                                List<String> lore = new ArrayList();
                                lore.add("");
                                lore.add(String.format("%sThis potion was brewed by", ChatColor.GRAY));
                                lore.add(String.format("%sa%s %s%s%s.", ChatColor.GRAY, this.StringStartsWithVowel(plyJob.getJob().getName()) ? "n" : "", ChatColor.GOLD, plyJob.getJob().getName(), ChatColor.RESET));
                                lore.add("");
                                if (duration > 0) {
                                    lore.add(String.format("%sDuration: %s%d:%02d", ChatColor.GRAY, ChatColor.GOLD, duration / 20 / 60, duration / 20 % 60));
                                } else {
                                    lore.add(String.format("%sInstant Potion%s", ChatColor.GOLD, ChatColor.RESET));
                                }

                                lore.add("");
                                _pMeta.setLore(lore);
                                ((ItemStack)event.getResults().get(i)).setItemMeta(_pMeta);
                            }
                        }
                    }

                    String experienceEq;
                    if (plyJob.getJob().getPaymentEnabled()) {
                        experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        double paymentAmount = 0.0;

                        try {
                            paymentAmount = new Double(sEngine.eval(experienceEq).toString()) * ((double)validPotions / 3.0);
                        } catch (ScriptException var22) {
                            var22.printStackTrace();
                        }

                        SpecialisedWorkforce.PayPlayer(ply.getUuid(), paymentAmount);
                    }

                    experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                    int expGain = 0;

                    try {
                        expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()) * ((double)validPotions / 3.0));
                    } catch (ScriptException var21) {
                        var21.printStackTrace();
                    }

                    WorkforceManager.AddExperiencePointsToPlayer(ply.getUuid(), plyJob.getJob().getName(), expGain);
                }
            }
        }

    }

    @EventHandler
    public void PlayerIsEnchanting(PrepareItemEnchantEvent event) {
        String plyUuid = event.getEnchanter().getUniqueId().toString();
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            PlayerJob plyJob;
            JobAttribute attr;
            ScriptEngine sEngine;
            String modifierEq;
            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST)) {
                plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST);
                attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_LEVEL_BOOST);
                if (attr != null) {
                    sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                    modifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    int amount = 0;

                    try {
                        amount = (int)Math.floor(new Double(sEngine.eval(modifierEq).toString()));
                    } catch (ScriptException var14) {
                        var14.printStackTrace();
                    }

                    int finalAmount = amount;

                    for(int i = 0; i < event.getOffers().length; ++i) {
                        if (event.getOffers()[i] != null) {
                            int lvl = event.getOffers()[i].getEnchantmentLevel() + finalAmount;
                            event.getOffers()[i].setEnchantmentLevel(lvl > event.getOffers()[i].getEnchantment().getMaxLevel() + 1 ? event.getOffers()[i].getEnchantment().getMaxLevel() + 1 : lvl);
                        }
                    }
                }
            }

            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION)) {
                plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION);
                attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_COST_REDUCTION);
                if (attr != null) {
                    sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
                    modifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double modifier = 0.0;

                    try {
                        modifier = Double.parseDouble(sEngine.eval(modifierEq).toString());
                    } catch (ScriptException var13) {
                        var13.printStackTrace();
                    }

                    double modifierVal = 1.0 - modifier;

                    for(int i = 0; i < event.getOffers().length; ++i) {
                        if (event.getOffers()[i] != null) {
                            event.getOffers()[i].setCost((int)Math.ceil((double)event.getOffers()[i].getCost() * modifierVal));
                        }
                    }
                }
            }
        }

    }

    @EventHandler
    public void ItemEnchantedHandler(EnchantItemEvent event) {
        String plyUuid = event.getEnchanter().getUniqueId().toString();
        if (WorkforceManager.PlayerIsEmployed(plyUuid)) {
            PlayerJobData ply = WorkforceManager.GetPlayerData(plyUuid);
            ScriptEngine sEngine = (new NashornScriptEngineFactory()).getScriptEngine();
            PlayerJob plyJob;
            JobAttribute attr;
            String levelModifierEq;
            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION)) {
                plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_COST_REDUCTION);
                attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_COST_REDUCTION);
                if (attr != null) {
                    levelModifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                    double modifier = 0.0;

                    try {
                        modifier = Double.parseDouble(sEngine.eval(levelModifierEq).toString());
                    } catch (ScriptException var17) {
                        var17.printStackTrace();
                    }

                    event.setExpLevelCost((int)Math.ceil((double)event.getExpLevelCost() * (1.0 - modifier)));
                    String experienceEq;
                    if (plyJob.getJob().getPaymentEnabled()) {
                        experienceEq = plyJob.getJob().getPaymentEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                        double amount = 0.0;

                        try {
                            amount = new Double(sEngine.eval(experienceEq).toString());
                        } catch (ScriptException var16) {
                            var16.printStackTrace();
                        }

                        SpecialisedWorkforce.PayPlayer(plyUuid, amount);
                    }

                    experienceEq = WorkforceManager.experienceGainEquation.replace("{level}", String.format("%d", plyJob.getLevel()));
                    int expGain = 0;

                    try {
                        expGain = (int)Math.floor(new Double(sEngine.eval(experienceEq).toString()));
                    } catch (ScriptException var15) {
                        var15.printStackTrace();
                    }

                    WorkforceManager.AddExperiencePointsToPlayer(ply.getUuid(), plyJob.getJob().getName(), expGain);
                }
            }

            if (ply.playerHasJobWithAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST)) {
                plyJob = ply.getPlayerJobByAttribute(JobAttributeType.ENCHANTING_LEVEL_BOOST);
                attr = plyJob.getJob().getAttributeByType(JobAttributeType.ENCHANTING_LEVEL_BOOST);
                levelModifierEq = attr.getEquation().replace("{level}", String.format("%d", plyJob.getLevel()));
                int amount = 0;

                try {
                    amount = (int)Math.ceil(Double.parseDouble(sEngine.eval(levelModifierEq).toString()));
                } catch (ScriptException var14) {
                    var14.printStackTrace();
                }

                Set<Enchantment> enchantmentKeys = event.getEnchantsToAdd().keySet();
                Iterator var19 = enchantmentKeys.iterator();

                while(var19.hasNext()) {
                    Enchantment _enc = (Enchantment)var19.next();
                    int combinedAmount = (Integer)event.getEnchantsToAdd().get(_enc) + amount;
                    event.getEnchantsToAdd().replace(_enc, combinedAmount > _enc.getMaxLevel() + 1 ? _enc.getMaxLevel() + 1 : combinedAmount);
                }
            }
        }

    }

    private int GetBaseGearScoreByType(Material type) {
        int score = 0;
        if (this.BaseScoreChart.size() == 0) {
            this.BaseScoreChart.put(Material.WOODEN_PICKAXE, 1);
            this.BaseScoreChart.put(Material.WOODEN_SHOVEL, 1);
            this.BaseScoreChart.put(Material.WOODEN_SWORD, 1);
            this.BaseScoreChart.put(Material.WOODEN_AXE, 1);
            this.BaseScoreChart.put(Material.WOODEN_HOE, 1);
            this.BaseScoreChart.put(Material.STONE_PICKAXE, 10);
            this.BaseScoreChart.put(Material.STONE_SHOVEL, 10);
            this.BaseScoreChart.put(Material.STONE_SWORD, 10);
            this.BaseScoreChart.put(Material.STONE_AXE, 15);
            this.BaseScoreChart.put(Material.STONE_HOE, 10);
            this.BaseScoreChart.put(Material.IRON_PICKAXE, 20);
            this.BaseScoreChart.put(Material.IRON_SHOVEL, 20);
            this.BaseScoreChart.put(Material.IRON_SWORD, 20);
            this.BaseScoreChart.put(Material.IRON_AXE, 25);
            this.BaseScoreChart.put(Material.IRON_HOE, 20);
            this.BaseScoreChart.put(Material.GOLDEN_PICKAXE, 15);
            this.BaseScoreChart.put(Material.GOLDEN_SHOVEL, 15);
            this.BaseScoreChart.put(Material.GOLDEN_SWORD, 15);
            this.BaseScoreChart.put(Material.GOLDEN_AXE, 20);
            this.BaseScoreChart.put(Material.GOLDEN_HOE, 15);
            this.BaseScoreChart.put(Material.DIAMOND_PICKAXE, 30);
            this.BaseScoreChart.put(Material.DIAMOND_SHOVEL, 30);
            this.BaseScoreChart.put(Material.DIAMOND_SWORD, 30);
            this.BaseScoreChart.put(Material.DIAMOND_AXE, 35);
            this.BaseScoreChart.put(Material.DIAMOND_HOE, 30);
            this.BaseScoreChart.put(Material.NETHERITE_PICKAXE, 40);
            this.BaseScoreChart.put(Material.NETHERITE_SHOVEL, 40);
            this.BaseScoreChart.put(Material.NETHERITE_SWORD, 40);
            this.BaseScoreChart.put(Material.NETHERITE_AXE, 45);
            this.BaseScoreChart.put(Material.NETHERITE_HOE, 40);
            this.BaseScoreChart.put(Material.BOW, 30);
            this.BaseScoreChart.put(Material.FISHING_ROD, 40);
            this.BaseScoreChart.put(Material.LEATHER_HELMET, 10);
            this.BaseScoreChart.put(Material.LEATHER_CHESTPLATE, 20);
            this.BaseScoreChart.put(Material.LEATHER_LEGGINGS, 15);
            this.BaseScoreChart.put(Material.LEATHER_BOOTS, 5);
            this.BaseScoreChart.put(Material.IRON_HELMET, 20);
            this.BaseScoreChart.put(Material.IRON_CHESTPLATE, 30);
            this.BaseScoreChart.put(Material.IRON_LEGGINGS, 25);
            this.BaseScoreChart.put(Material.IRON_BOOTS, 15);
            this.BaseScoreChart.put(Material.GOLDEN_HELMET, 15);
            this.BaseScoreChart.put(Material.GOLDEN_CHESTPLATE, 25);
            this.BaseScoreChart.put(Material.GOLDEN_LEGGINGS, 20);
            this.BaseScoreChart.put(Material.GOLDEN_BOOTS, 10);
            this.BaseScoreChart.put(Material.CHAINMAIL_HELMET, 25);
            this.BaseScoreChart.put(Material.CHAINMAIL_CHESTPLATE, 35);
            this.BaseScoreChart.put(Material.CHAINMAIL_LEGGINGS, 30);
            this.BaseScoreChart.put(Material.CHAINMAIL_BOOTS, 20);
            this.BaseScoreChart.put(Material.DIAMOND_HELMET, 35);
            this.BaseScoreChart.put(Material.DIAMOND_CHESTPLATE, 45);
            this.BaseScoreChart.put(Material.DIAMOND_LEGGINGS, 40);
            this.BaseScoreChart.put(Material.DIAMOND_BOOTS, 30);
            this.BaseScoreChart.put(Material.NETHERITE_HELMET, 45);
            this.BaseScoreChart.put(Material.NETHERITE_CHESTPLATE, 55);
            this.BaseScoreChart.put(Material.NETHERITE_LEGGINGS, 50);
            this.BaseScoreChart.put(Material.NETHERITE_BOOTS, 30);
        }

        score = (Integer)this.BaseScoreChart.getOrDefault(type, 0);
        return score;
    }

    private int GetGearScoreByEnchantmentType(Enchantment type, int level) {
        int score = 0;
        if (this.EnchantmentScoreChart.size() == 0) {
            this.EnchantmentScoreChart.put(Enchantment.DURABILITY, 10);
            this.EnchantmentScoreChart.put(Enchantment.MENDING, 30);
            this.EnchantmentScoreChart.put(Enchantment.BINDING_CURSE, -5);
            this.EnchantmentScoreChart.put(Enchantment.VANISHING_CURSE, -20);
            this.EnchantmentScoreChart.put(Enchantment.DIG_SPEED, 15);
            this.EnchantmentScoreChart.put(Enchantment.LOOT_BONUS_BLOCKS, 10);
            this.EnchantmentScoreChart.put(Enchantment.LUCK, 10);
            this.EnchantmentScoreChart.put(Enchantment.LURE, 10);
            this.EnchantmentScoreChart.put(Enchantment.SILK_TOUCH, 20);
            this.EnchantmentScoreChart.put(Enchantment.ARROW_DAMAGE, 5);
            this.EnchantmentScoreChart.put(Enchantment.ARROW_FIRE, 10);
            this.EnchantmentScoreChart.put(Enchantment.ARROW_INFINITE, 20);
            this.EnchantmentScoreChart.put(Enchantment.ARROW_KNOCKBACK, 5);
            this.EnchantmentScoreChart.put(Enchantment.CHANNELING, 10);
            this.EnchantmentScoreChart.put(Enchantment.DAMAGE_ALL, 15);
            this.EnchantmentScoreChart.put(Enchantment.DAMAGE_ARTHROPODS, 5);
            this.EnchantmentScoreChart.put(Enchantment.DAMAGE_UNDEAD, 10);
            this.EnchantmentScoreChart.put(Enchantment.FIRE_ASPECT, 10);
            this.EnchantmentScoreChart.put(Enchantment.IMPALING, 5);
            this.EnchantmentScoreChart.put(Enchantment.KNOCKBACK, 5);
            this.EnchantmentScoreChart.put(Enchantment.LOOT_BONUS_MOBS, 10);
            this.EnchantmentScoreChart.put(Enchantment.LOYALTY, 20);
            this.EnchantmentScoreChart.put(Enchantment.MULTISHOT, 10);
            this.EnchantmentScoreChart.put(Enchantment.PIERCING, 5);
            this.EnchantmentScoreChart.put(Enchantment.QUICK_CHARGE, 15);
            this.EnchantmentScoreChart.put(Enchantment.SWEEPING_EDGE, 10);
            this.EnchantmentScoreChart.put(Enchantment.RIPTIDE, 10);
            this.EnchantmentScoreChart.put(Enchantment.DEPTH_STRIDER, 10);
            this.EnchantmentScoreChart.put(Enchantment.FROST_WALKER, 10);
            this.EnchantmentScoreChart.put(Enchantment.OXYGEN, 15);
            this.EnchantmentScoreChart.put(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
            this.EnchantmentScoreChart.put(Enchantment.PROTECTION_EXPLOSIONS, 20);
            this.EnchantmentScoreChart.put(Enchantment.PROTECTION_FALL, 10);
            this.EnchantmentScoreChart.put(Enchantment.PROTECTION_FIRE, 20);
            this.EnchantmentScoreChart.put(Enchantment.PROTECTION_PROJECTILE, 15);
            this.EnchantmentScoreChart.put(Enchantment.SOUL_SPEED, 20);
            this.EnchantmentScoreChart.put(Enchantment.SWIFT_SNEAK, 15);
            this.EnchantmentScoreChart.put(Enchantment.THORNS, 10);
            this.EnchantmentScoreChart.put(Enchantment.WATER_WORKER, 15);
        }

        score = (Integer)this.EnchantmentScoreChart.getOrDefault(type, 0) * level;
        return score;
    }

    private String ConvertTypeToName(String type, String delimiter) {
        String str = "";
        String[] parts = type.toString().split(delimiter);
        String[] var5 = parts;
        int var6 = parts.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String p = var5[var7];
            str = String.format("%s %s%s", str, p.substring(0, 1).toUpperCase(), p.substring(1).toLowerCase());
        }

        return str;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (SQLManager.TrackPlayerGear && SQLManager.Connect()) {
            try {
                PreparedStatement deletePlayerGearStmt = SQLManager.GetConnection().prepareStatement("DELETE FROM PlayerGear WHERE item_owner=?;");
                deletePlayerGearStmt.setString(1, event.getPlayer().getUniqueId().toString());
                deletePlayerGearStmt.executeUpdate();
                deletePlayerGearStmt.close();
            } catch (SQLException var14) {
                var14.printStackTrace();
            }

            Player ply = event.getPlayer();
            PlayerInventory plyInv = ply.getInventory();
            String[] validItems = new String[]{"_pickaxe", "_shovel", "_sword", "_axe", "_hoe", "bow", "crossbow", "_helmet", "_chestplate", "_leggings", "_boots"};
            List<ItemStack> itemsToSave = new ArrayList();
            ItemStack[] var6 = plyInv.getContents();
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                ItemStack item = var6[var8];
                if (item != null && Arrays.stream(validItems).anyMatch((_i) -> {
                    return item.getType().toString().toLowerCase().endsWith(_i);
                })) {
                    itemsToSave.add(item);
                }
            }

            try {
                Iterator var17 = itemsToSave.iterator();

                label56:
                while(true) {
                    ItemStack item;
                    do {
                        if (!var17.hasNext()) {
                            break label56;
                        }

                        item = (ItemStack)var17.next();
                    } while(item == null);

                    PreparedStatement addToPlayerGear = SQLManager.GetConnection().prepareStatement("INSERT INTO PlayerGear(item_owner, item_name, base_gear_score, item_type) VALUES (?, ?, ?, ?);", 1);
                    addToPlayerGear.setString(1, ply.getUniqueId().toString());
                    addToPlayerGear.setString(2, item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : this.ConvertTypeToName(item.getType().toString(), "_"));
                    addToPlayerGear.setInt(3, this.GetBaseGearScoreByType(item.getType()));
                    addToPlayerGear.setString(4, item.getType().toString().toLowerCase());
                    addToPlayerGear.executeUpdate();
                    ResultSet keys = addToPlayerGear.getGeneratedKeys();
                    keys.next();
                    int newId = keys.getInt(1);
                    addToPlayerGear.close();
                    Iterator var11 = item.getEnchantments().keySet().iterator();

                    while(var11.hasNext()) {
                        Enchantment enc = (Enchantment)var11.next();
                        PreparedStatement gearEncStmt = SQLManager.GetConnection().prepareStatement("INSERT INTO GearEnchantment VALUES(?, ?, ?);");
                        gearEncStmt.setInt(1, newId);
                        gearEncStmt.setString(2, String.format("%s%s", this.ConvertTypeToName(enc.getKey().getKey(), "_"), enc.getMaxLevel() > 1 ? " " + Helpers.toRoman((Integer)item.getEnchantments().get(enc)) : ""));
                        gearEncStmt.setInt(3, this.GetGearScoreByEnchantmentType(enc, (Integer)item.getEnchantments().get(enc)));
                        gearEncStmt.executeUpdate();
                        gearEncStmt.close();
                    }
                }
            } catch (SQLException var15) {
                var15.printStackTrace();
            }

            SQLManager.Disconnect();
        }

    }
}
