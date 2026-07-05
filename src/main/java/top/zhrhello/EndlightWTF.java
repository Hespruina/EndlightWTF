package top.zhrhello;

import org.bukkit.NamespacedKey;
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
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Sound;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EndlightWTF extends JavaPlugin implements Listener {

    private static EndlightWTF instance;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MilkBucketListener(this), this);
        createRecipe();
    }

    private void createRecipe() {
        NamespacedKey key = new NamespacedKey(this, "endlightwtf_lubed_end_rod");

        if (Bukkit.getRecipe(key) != null) {
            getLogger().info("润滑末地烛配方已存在，跳过注册");
            return;
        }

        ItemStack result = new ItemStack(Material.END_ROD);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName("§b润滑末地烛");
        meta.setLore(java.util.Arrays.asList("§7插进别人的身体......或者自己用"));
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        result.setItemMeta(meta);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.SLIME_BALL);
        recipe.addIngredient(Material.END_ROD);
        Bukkit.addRecipe(recipe);

        getLogger().info("润滑末地烛无序配方已注册");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();
        if (!(targetEntity instanceof Player)) return;
        Player target = (Player) targetEntity;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isLubedEndRod(item)) return;
        handleInteraction(player, target);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isLubedEndRod(item)) return;

        handleInteraction(player, player);
    }

    private boolean isLubedEndRod(ItemStack item) {
        if (item == null || item.getType() != Material.END_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && "§b润滑末地烛".equals(meta.getDisplayName());
    }

    private void handleInteraction(Player caster, Player target) {
        UUID casterId = caster.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldown.containsKey(casterId) && now - cooldown.get(casterId) < 10000) {
            caster.sendMessage(Component.text("贤者时间！").color(NamedTextColor.RED));
            return;
        }

        // 给目标穿普通末地烛（无自定义名）
        placePlainEndRodOnLegs(target);

        // 回血
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
            caster.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    private void placePlainEndRodOnLegs(Player target) {
        ItemStack legArmor = target.getInventory().getLeggings();
        ItemStack endRod = new ItemStack(Material.END_ROD); // 普通末地烛

        if (legArmor == null || legArmor.getType() == Material.AIR) {
            target.getInventory().setLeggings(endRod);
        } else {
            if (addItemToInventory(target, legArmor)) {
                target.getInventory().setLeggings(endRod);
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), legArmor);
                target.getInventory().setLeggings(endRod);
            }
        }
    }

    private boolean addItemToInventory(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            return true;
        }
        if (player.getInventory().getItemInOffHand().getType() == Material.AIR) {
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
        meta.setDisplayName("§f" + producer + "§f为§f" + consumer + "§f生产的牛奶");
        meta.setLore(java.util.Arrays.asList("§7" + producer + " 与 " + consumer + " 生产的牛奶"));
        milk.setItemMeta(meta);

        if (!addItemToInventory(caster, milk)) {
            caster.getWorld().dropItemNaturally(caster.getLocation(), milk);
        }
    }

    private void applyRegen(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 1)); // 30秒，II级
    }

    public static EndlightWTF getInstance() {
        return instance;
    }
}