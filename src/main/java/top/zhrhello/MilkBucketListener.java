package top.zhrhello;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MilkBucketListener implements Listener {
    
    private final EndlightWTF plugin;
    
    public MilkBucketListener(EndlightWTF plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onMilkDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        
        // 检查是否是自定义牛奶
        if (item.getType() == Material.MILK_BUCKET && item.hasItemMeta()) {
            // 检查是否有自定义名称
            if (item.getItemMeta().hasDisplayName() && 
                item.getItemMeta().getDisplayName().contains(" 的牛奶")) {
                
                // 取消默认的清除效果行为
                event.setCancelled(true);
                
                // 手动清除玩家的所有药水效果
                event.getPlayer().getActivePotionEffects().forEach(effect -> {
                    event.getPlayer().removePotionEffect(effect.getType());
                });
                
                // 添加眩晕效果
                event.getPlayer().addPotionEffect(new PotionEffect(
                    PotionEffectType.CONFUSION,
                    10 * 20, // 10秒
                    0,
                    false,
                    true
                ));
                
                // 将牛奶桶替换为空桶
                event.getPlayer().getInventory().setItemInHand(new ItemStack(Material.BUCKET));
            }
        }
    }
}
