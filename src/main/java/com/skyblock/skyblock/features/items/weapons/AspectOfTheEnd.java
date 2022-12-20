package com.skyblock.skyblock.features.items.weapons;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.SkyblockPlayer;
import com.skyblock.skyblock.enums.SkyblockStat;
import com.skyblock.skyblock.features.items.SkyblockItem;
import com.skyblock.skyblock.utilities.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Set;

public class AspectOfTheEnd extends SkyblockItem {

    public AspectOfTheEnd(Skyblock plugin) {
        super(plugin.getItemHandler().getItem("ASPECT_OF_THE_END.json"), "aspect_of_the_end");
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, HashMap<String, Object> data) {
        Player player = event.getPlayer();

        SkyblockPlayer skyblockPlayer = SkyblockPlayer.getPlayer(player);

        if (skyblockPlayer.checkMana(50)) {
            Location location = player.getTargetBlock((Set<Material>) null, 8).getLocation();
            location.setYaw(player.getLocation().getYaw());
            location.setPitch(player.getLocation().getPitch());
            player.teleport(location);

            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 3f, 1f);

            if (skyblockPlayer.getCooldown(getInternalName())) {
                skyblockPlayer.addStat(SkyblockStat.SPEED, 50);
                skyblockPlayer.setCooldown(getInternalName(), 3);

                skyblockPlayer.delay(() -> skyblockPlayer.subtractStat(SkyblockStat.SPEED, 50), 3);
            }

            Util.sendAbility(skyblockPlayer, "Instant Transmission", 50);
        }
    }
}
