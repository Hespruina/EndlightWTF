package top.zhrhello;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Sound;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LubricatedEndRod extends JavaPlugin implements Listener {

    private static LubricatedEndRod instance;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    
    // 自定义牛奶名称后缀标识
    private static final String MILK_NAME_SUFFIX = "的牛奶";

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MilkBucketListener(this), this);
        createRecipe();
        getLogger().info("LubricatedEndRod 插件已启用");
    }
    
    @Override
    public void onDisable() {
        // 清理冷却记录
        cooldown.clear();
        getLogger().info("LubricatedEndRod 插件已禁用");
    }

    private void createRecipe() {
        NamespacedKey key = new NamespacedKey(this, "lubricatedendrod_lubed_end_rod");

        if (Bukkit.getRecipe(key) != null) {
            getLogger().info("润滑末地烛配方已存在，跳过注册");
            return;
        }

        ItemStack result = new ItemStack(Material.END_ROD);
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;
        
        meta.displayName(Component.text("润滑末地烛").color(NamedTextColor.AQUA));
        meta.lore(Collections.singletonList(
            Component.text("插进别人的身体......或者自己用").color(NamedTextColor.GRAY)
        ));
        
        // 获取耐久附魔 - 兼容不同版本
        Enchantment durabilityEnchant = getDurabilityEnchantment();
        if (durabilityEnchant != null) {
            meta.addEnchant(durabilityEnchant, 1, true);
        }
        
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        result.setItemMeta(meta);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.SLIME_BALL);
        recipe.addIngredient(Material.END_ROD);
        Bukkit.addRecipe(recipe);

        getLogger().info("润滑末地烛无序配方已注册");
    }
    
    private Enchantment getDurabilityEnchantment() {
        // 尝试不同的方式获取耐久/不灭附魔
        try {
            // 新版 API (1.21+)
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
        } catch (NoSuchMethodError | Exception e1) {
            try {
                // 旧版 API
                return Enchantment.getByName("DURABILITY");
            } catch (NoSuchMethodError | Exception e2) {
                try {
                    // 更旧的版本
                    return Enchantment.getByName("UNBREAKING");
                } catch (Exception e3) {
                    getLogger().warning("无法获取耐久附魔，将不添加附魔效果");
                    return null;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();
        if (!(targetEntity instanceof Player targetPlayer)) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isLubedEndRod(item)) return;
        
        event.setCancelled(true);
        handleInteraction(player, targetPlayer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isLubedEndRod(item)) return;
        
        event.setCancelled(true);
        handleInteraction(player, player);
    }

    private boolean isLubedEndRod(ItemStack item) {
        if (item == null || item.getType() != Material.END_ROD) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && 
               meta.getDisplayName().contains("润滑末地烛");
    }

    private void handleInteraction(Player caster, Player target) {
        UUID casterId = caster.getUniqueId();
        long now = System.currentTimeMillis();
        
        // 检查冷却
        if (cooldown.containsKey(casterId) && now - cooldown.get(casterId) < 10000) {
            caster.sendMessage(Component.text("贤者时间！").color(NamedTextColor.RED));
            return;
        }

        // 给目标穿普通末地烛（无自定义名）
        placePlainEndRodOnLegs(target);

        // 应用生命恢复效果
        applyRegen(caster);
        applyRegen(target);

        // 播放饮用音效
        caster.playSound(caster.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        if (!caster.equals(target)) {
            target.playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        }

        // 给施法者牛奶
        giveMilkToCaster(caster, target);

        // 设置冷却
        cooldown.put(casterId, now);

        // 一次性消耗（非创造模式）
        if (caster.getGameMode() != GameMode.CREATIVE) {
            caster.getInventory().setItemInMainHand(ItemStack.empty());
        }
    }

    private void placePlainEndRodOnLegs(Player target) {
        ItemStack legArmor = target.getInventory().getLeggings();
        ItemStack endRod = new ItemStack(Material.END_ROD); // 普通末地烛

        if (legArmor == null || legArmor.getType() == Material.AIR) {
            target.getInventory().setLeggings(endRod);
        } else {
            // 尝试将原护腿放入背包
            if (!addItemToInventory(target, legArmor)) {
                // 背包满则在原地掉落
                target.getWorld().dropItemNaturally(target.getLocation(), legArmor);
            }
            target.getInventory().setLeggings(endRod);
        }
    }

    private boolean addItemToInventory(Player player, ItemStack item) {
        // 检查主背包是否有空位
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            return true;
        }
        
        // 检查副手是否为空
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.isEmpty()) {
            player.getInventory().setItemInOffHand(item);
            return true;
        }
        
        return false;
    }

    private void giveMilkToCaster(Player caster, Player target) {
        String producer = target.getName();
        String consumer = caster.getName();
        
        ItemStack milk = new ItemStack(Material.MILK_BUCKET);
        ItemMeta meta = milk.getItemMeta();
        if (meta == null) return;
        
        meta.displayName(Component.text(producer + "为" + consumer + "生产的牛奶").color(NamedTextColor.WHITE));
        meta.lore(Collections.singletonList(
            Component.text(producer + " 与 " + consumer + " 生产的牛奶").color(NamedTextColor.GRAY)
        ));
        milk.setItemMeta(meta);

        if (!addItemToInventory(caster, milk)) {
            caster.getWorld().dropItemNaturally(caster.getLocation(), milk);
        }
    }

    private void applyRegen(Player player) {
        // 移除旧的生命恢复效果
        player.removePotionEffect(PotionEffectType.REGENERATION);
        // 添加新的生命恢复效果：600 ticks = 30秒，amplifier = 1 代表等级 II
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION, 
            600, 
            1, 
            false,  // ambient
            true,   // particles
            true    // icon
        ));
    }

    public static LubricatedEndRod getInstance() {
        return instance;
    }
    
    public static String getMilkNameSuffix() {
        return MILK_NAME_SUFFIX;
    }
}
