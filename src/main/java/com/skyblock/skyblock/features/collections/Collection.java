package com.skyblock.skyblock.features.collections;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.SkyblockPlayer;
import com.skyblock.skyblock.utilities.Util;
import com.skyblock.skyblock.utilities.chat.ChatMessageBuilder;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.MojangsonParseException;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Getter
public class Collection {

    private final HashMap<Integer, Integer> levelToExp;
    private final CollectionRewards rewards;
    private final Material material;
    private final String category;
    private final int maxLevel;
    private final String name;
    private final short data;

    public static boolean INITIALIZED = false;
    private static final List<Collection> collections = new ArrayList<>();

    public Collection(String name, Material material, short data, String category, int maxLevel, CollectionRewards rewards, int... levelToExp) {
        this.maxLevel = maxLevel;
        this.material = material;
        this.category = category;
        this.data = data;
        this.name = name;

        this.levelToExp = new HashMap<>();

        this.rewards = rewards;

        for (int i = 0; i < levelToExp.length; i++) this.levelToExp.put(i, levelToExp[i]);
    }

    public void collect(Player player, int amount) {
        SkyblockPlayer skyblockPlayer = new SkyblockPlayer(player.getUniqueId());

        int level = (int) skyblockPlayer.getValue("collection." + this.name.toLowerCase() + ".level");
        int exp = (int) skyblockPlayer.getValue("collection." + this.name.toLowerCase() + ".exp");

        int newExp = exp + amount;

        skyblockPlayer.setValue("collection." + this.name.toLowerCase() + ".exp", newExp);

        if (skyblockPlayer.getValue("collection." + this.name.toLowerCase() + ".unlocked").equals(false)) {
            skyblockPlayer.setValue("collection." + this.name.toLowerCase() + ".unlocked", true);

            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "  COLLECTION UNLOCKED " + ChatColor.YELLOW + this.name);

            return;
        }

        if (level == maxLevel || newExp>= levelToExp.get(0) || newExp >= levelToExp.get(level)) {
            this.rewards.reward(player, level);

            skyblockPlayer.setValue("collection." + this.name.toLowerCase() + ".level", level + 1);

            ChatMessageBuilder builder = new ChatMessageBuilder();

            builder
                    .add("&e&l&m================================")
                    .add("&6&l  COLLECTION LEVEL UP &e" + StringUtils.capitalize(this.name.toLowerCase()) + " &8" + (level == 0 ? 0 : Util.toRoman(level)) + " ➜ &e" + Util.toRoman(level + 1))
                    .add("")
                    .add("&a&l  REWARDS");

            for (String s : this.rewards.stringify(level + 1)) builder.add("  " + s);

            if (this.rewards.stringify(level + 1).size() == 0) builder.add("    &cComing soon");

            builder.add("&e&l&m================================");

            builder.build(player);
        }
    }

    public static void initializeCollections(Skyblock skyblock) {
        skyblock.sendMessage("Initializing collections...");

        Collection.INITIALIZED = true;

        File folder = new File(skyblock.getDataFolder() + File.separator + "collections");

        if (!folder.exists()) folder.mkdirs();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            try {
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)));
                JSONObject object = (JSONObject) obj;

                String category = (String) object.get("category");

                boolean found = false;

                for (CollectionCategory cat : Collection.getCollectionCategories()) {
                    if (cat.getName().equalsIgnoreCase(category)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    skyblock.sendMessage("&cFailed to initialize collection file &8" + file.getName() + "&c: category &8" + category + "&c does not exist");
                    continue;
                }

                JSONArray collections = (JSONArray) object.get("collections");

                for (Object collectionObject : collections) {
                    JSONObject collection = (JSONObject) collectionObject;

                    String name = (String) collection.get("name");
                    Material material = Material.valueOf((String) collection.get("material"));
                    short data = ((Long) collection.get("data")).shortValue();

                    JSONArray rewardsList = (JSONArray) collection.get("rewards");
                    HashMap<Integer, String> rewards = new HashMap<>();

                    int r = 0;

                    int[] levelToExp = new int[rewardsList.size()];

                    for (Object reward : rewardsList) {
                        String[] split = ((String) reward).split(";");
                        rewards.put(r, split[1]);

                        levelToExp[r] = Integer.parseInt(split[0]);

                        r++;
                    }

                    JSONArray commandsList = (JSONArray) collection.get("commands");
                    HashMap<Integer, String> commands = new HashMap<>();

                    int cmd = 0;

                    for (Object command : commandsList) {
                        commands.put(cmd, (String) command);

                        cmd++;
                    }

                    CollectionRewards.Reward[] rewardsArray = new CollectionRewards.Reward[rewards.size()];

                    for (int i = 0; i < rewards.size(); i++) {
                        int level = i + 1;

                        String reward = ChatColor.translateAlternateColorCodes('&', rewards.get(i));

                        String command = commands.get(i);

                        rewardsArray[i] = new CollectionRewards.Reward(reward, command, level);
                    }

                    CollectionRewards collectionRewards = new CollectionRewards(rewardsArray);

                    Collection generated = new Collection(name, material, data, category, levelToExp.length, collectionRewards, levelToExp);

                    Collection.collections.add(generated);
                }
            } catch (ParseException | IOException ex) {
                throw new RuntimeException("Failed to initialize collection file " + file.getName() + ": " + ex.getMessage());
            }
        }

        skyblock.sendMessage("Initialized " + Collection.collections.size() + " collections");
    }

    public static List<Collection> getCollections() {
        if (Collection.INITIALIZED) return Collection.collections;

        throw new RuntimeException("Collections not initialized");
    }

    public static List<CollectionCategory> getCollectionCategories() {
        List<CollectionCategory> categories = new ArrayList<>();

        categories.add(new CollectionCategory("Farming", Material.GOLD_HOE));
        categories.add(new CollectionCategory("Mining", Material.STONE_PICKAXE));
        categories.add(new CollectionCategory("Combat", Material.STONE_SWORD));
        categories.add(new CollectionCategory("Foraging", Material.SAPLING, (short) 3));
        categories.add(new CollectionCategory("Fishing", Material.FISHING_ROD));

        return categories;
    }

}