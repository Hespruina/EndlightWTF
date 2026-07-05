package top.zhrhello;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MilkBucketListener implements Listener {

    private final LubricatedEndRod plugin;

    public MilkBucketListener(LubricatedEndRod plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMilkDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        // 检查是否是牛奶桶
        if (item.getType() != Material.MILK_BUCKET) return;

        // 检查是否有物品元数据
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 检查是否是自定义牛奶
        if (!meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();
        if (!displayName.contains(LubricatedEndRod.getMilkNameSuffix())) return;

        // 取消默认的饮用行为
        event.setCancelled(true);

        // 清除所有药水效果（不包括不灭之火等特殊效果）
        clearAllPotionEffects(player);

        // 添加反胃效果（眩晕）
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.CONFUSION,
            200,    // 10秒 = 200 ticks
            0,      // 等级 I
            false,  // ambient
            true,   // particles
            true    // icon
        ));

        // 播放饮用音效
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);

        // 替换手中的牛奶桶为空桶
        replaceMilkWithBucket(player);
    }

    private void clearAllPotionEffects(Player player) {
        // 获取所有活跃的药水效果并移除
        player.getActivePotionEffects().forEach(effect -> {
            PotionEffectType type = effect.getType();
            // 保留不灭之火效果（如果有）
            if (type != PotionEffectType.FIRE_RESISTANCE || 
                !player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                player.removePotionEffect(type);
            }
        });
    }

    private void replaceMilkWithBucket(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // 检查主手
        if (mainHand.getType() == Material.MILK_BUCKET && hasCustomMilkName(mainHand)) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
            return;
        }

        // 检查副手
        if (offHand.getType() == Material.MILK_BUCKET && hasCustomMilkName(offHand)) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.BUCKET));
        }
    }

    private boolean hasCustomMilkName(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && 
               meta.getDisplayName().contains(LubricatedEndRod.getMilkNameSuffix());
    }
}