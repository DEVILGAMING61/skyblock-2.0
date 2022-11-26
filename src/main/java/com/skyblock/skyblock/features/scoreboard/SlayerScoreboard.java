package com.skyblock.skyblock.features.scoreboard;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.SkyblockPlayer;
import com.skyblock.skyblock.features.island.IslandManager;
import com.skyblock.skyblock.features.location.SkyblockLocation;
import com.skyblock.skyblock.features.slayer.SlayerBoss;
import com.skyblock.skyblock.features.slayer.SlayerHandler;
import com.skyblock.skyblock.features.slayer.SlayerQuest;
import com.skyblock.skyblock.utilities.Util;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class SlayerScoreboard extends Scoreboard {

    public SlayerScoreboard(Player player) {
        super(player);
    }

    @Override
    void display() {
        SkyblockPlayer skyblockPlayer = SkyblockPlayer.getPlayer(player);

        DecimalFormat formatter = new DecimalFormat("#,###");
        formatter.setGroupingUsed(true);

        String date = new SimpleDateFormat("MM/dd/yy").format(Skyblock.getPlugin(Skyblock.class).getDate());

        SkyblockLocation currentLocation = skyblockPlayer.getCurrentLocation();

        String loc = currentLocation == null ? ChatColor.GRAY + "None" : currentLocation.getColor() + currentLocation.getName();

        if (currentLocation == null) loc = ChatColor.GRAY + "None";

        if (player.getWorld().getName().startsWith(IslandManager.ISLAND_PREFIX)) loc = ChatColor.GREEN + "Private Island";

        addLine(13, ChatColor.GRAY + "" + date + ChatColor.DARK_GRAY + " Skyblock");
        addLine(12, ChatColor.GRAY + "   ");
        addLine(11, ChatColor.WHITE + " " + Skyblock.getPlugin(Skyblock.class).getTimeManager().getDate());
        addLine(10, ChatColor.GRAY + " " + Skyblock.getPlugin(Skyblock.class).getTimeManager().getTime() + " " + Skyblock.getPlugin(Skyblock.class).getTimeManager().getIcon());
        if (!loc.contains("Private Island")) {
            if (currentLocation != null) addLine(9, currentLocation.getColor() + " ⏣ " + loc);
            else addLine(9, ChatColor.WHITE + " ⏣ " + loc);
        } else {
            addLine(9, ChatColor.GREEN + " ⏣ " + loc);
        }
        addLine(8, ChatColor.WHITE + " ");
        addLine(7, ChatColor.WHITE + "Purse: " + ChatColor.GOLD +  formatter.format((int) skyblockPlayer.getValue("stats.purse")));
        addLine(6, ChatColor.WHITE + "   ");
        addLine(5, ChatColor.WHITE + "Slayer Quest");

        SlayerHandler.SlayerData data = (SlayerHandler.SlayerData) skyblockPlayer.getExtraData("slayerData");
        SlayerBoss boss = data.getBoss();
        SlayerQuest quest = data.getQuest();

        addLine(4, ChatColor.DARK_RED + boss.getEntityData().entityName + " " + Util.toRoman(boss.getLevel()));

        switch (quest.getState()) {
            case SUMMONING:
                addLine(3, ChatColor.GRAY + " (" + ChatColor.YELLOW + Util.formatInt(quest.getExp()) + ChatColor.GRAY + "/" + ChatColor.RED + Util.formatInt(quest.getNeededExp()) + ChatColor.GRAY + ")");
                break;
            case FIGHTING:
                addLine(3, ChatColor.YELLOW + "Slay the boss!");
                break;
            case FINISHED:
                addLine(3, ChatColor.GREEN + "Boss slain!");
        }
        addLine(2, ChatColor.WHITE + "  ");
        addLine(1, ChatColor.YELLOW + "www.hypixel.net");
    }
}