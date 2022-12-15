package com.skyblock.skyblock.features.enchantment.enchantments.sword;

import com.skyblock.skyblock.SkyblockPlayer;
import com.skyblock.skyblock.features.enchantment.types.SwordEnchantment;
import com.skyblock.skyblock.utilities.item.ItemBase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class CleaveEnchantment extends SwordEnchantment {

    public CleaveEnchantment() {
        super("cleave", "Cleave", (level) -> {
            return "&7Deals " + ChatColor.GREEN + level * 3 + "% &7of your damage\n&7dealth to other mosters within\n" + ChatColor.GREEN + Math.round((3.3 + (level - 1) * 0.3)) + "&7 blocks of the target";
        }, 4);
    }

    @Override
    public void onDamage(SkyblockPlayer player, EntityDamageByEntityEvent e, double damage) {
        if (player.getExtraData("cleave_enchantment") != null) return;

        try {
            ItemBase base = new ItemBase(player.getBukkitPlayer().getItemInHand());
            int level = base.getEnchantment(this.getName()).getLevel();
            Entity entity = e.getEntity();

            for (Entity en : entity.getNearbyEntities((3.3 + (level - 1) * 0.3), 2, (3.3 + (level - 1) * 0.3))) {
                if (en instanceof Player) continue;

                player.setExtraData("cleave_enchantment", (level * 3) / 100F);
                Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(player.getBukkitPlayer(), en, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage));
                ((LivingEntity) en).damage(0);
                player.setExtraData("cleave_enchantment", null);
            }
        }  catch (IllegalArgumentException | NullPointerException ignored) { }
    }
}