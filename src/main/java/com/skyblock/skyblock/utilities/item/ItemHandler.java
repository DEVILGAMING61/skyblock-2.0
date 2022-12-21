package com.skyblock.skyblock.utilities.item;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.enums.Rarity;
import com.skyblock.skyblock.enums.Reforge;
import com.skyblock.skyblock.features.crafting.SkyblockRecipe;
import com.skyblock.skyblock.features.enchantment.ItemEnchantment;
import com.skyblock.skyblock.features.enchantment.SkyblockEnchantment;
import com.skyblock.skyblock.features.enchantment.SkyblockEnchantmentHandler;
import com.skyblock.skyblock.features.pets.Pet;
import com.skyblock.skyblock.features.pets.PetType;
import com.skyblock.skyblock.utilities.Util;
import de.tr7zw.nbtapi.NBTItem;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.Enchantment;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.MojangsonParseException;
import net.minecraft.server.v1_8_R3.MojangsonParser;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import java.util.function.Function;

@Getter
public class ItemHandler {
    private final HashMap<String, ItemStack> items = new HashMap<>();
    private final HashMap<ItemStack, String> reversed = new HashMap<>();
    private final Skyblock skyblock;

    public static final List<String> ITEM_EXCLUSIONS = new ArrayList<String>() {{
        add("fancy_sword.json");
        add("raider_axe.json");
        add("midas_sword.json");
    }};

    public static final HashMap<String, Function<ItemBase, ItemBase>> SKYBLOCK_ID_TO_ITEM = new HashMap<String, Function<ItemBase, ItemBase>>() {{
        put("raider_axe", (base -> {
            NBTItem nbtItem = new NBTItem(base.createStack().clone());

            List<String> description = Arrays.asList(Util.buildLore(
                    "&7Earn &6+20 coins &7from monster kills (level &e10+ &7only)\n\n&c+1 Damage &7per &c500 &7kills\n&8Max +35\n&7Kills: &c" + Util.formatInt(nbtItem.getInteger("raider_axe_kills")) +
                            "\n\n&c+1❁ Strength &7per &e500 &7wood\n&8Sums wood collections, max +100\n&7Wood collections: &e" + Util.formatInt(nbtItem.getInteger("raider_axe_collection")) + "\n" + " ")
            );

            NBTItem nbt = new NBTItem(base.getStack());

            int currentKills = nbt.getInteger("raider_axe_kills");
            int appliedKills = 0;

            int damage = 0;

            for (int i = 0; i < currentKills / 500; i++) {
                if (appliedKills >= 35) break;

                damage++;
                appliedKills++;
            }

            base.setDamage(base.getDamage() + damage);

            base.setDescription(description);
            base.createStack();

            nbt = new NBTItem(base.getStack());

            nbt.setInteger("raider_axe_kills", nbtItem.getInteger("raider_axe_kills"));
            nbt.setInteger("raider_axe_collection", nbtItem.getInteger("raider_axe_collection"));

            base.setStack(nbt.getItem());

            return base;
        }));

        put("midas_sword", (base) -> {
            NBTItem nbtItem = new NBTItem(base.createStack().clone());

            boolean alreadyApplied = nbtItem.getBoolean("midas_sword_applied");
            int pricePaid = nbtItem.getInteger("midas_sword_price_paid");
            int bonus;

            if (pricePaid < 1_000_000) bonus = (pricePaid / 50_000);
            else if (pricePaid < 2_500_000) bonus = ((pricePaid - 1_000_000) / 100_000);
            else if (pricePaid < 7_500_000) bonus = ((pricePaid - 2_500_000) / 200_000);
            else if (pricePaid < 25_000_000) bonus = ((pricePaid - 7_500_000) / 500_000);
            else if (pricePaid < 50_000_000) bonus = ((pricePaid - 25_000_000) / 1_000_000);
            else bonus = 120;

            base.setAbilityName("Greed");
            base.setAbilityDescription(Arrays.asList(Util.buildLore(
                    "&7The strength and damage bonus of\n&7his item is dependent on the\n&7price paid for it at the &5Dark\n&5Auction&7!\n&7The maximum bonus of this item is &c120 &7if the bid was\n&650,000,000 Coins&7 or higher!" +
                            "\n\n&7Price paid: &6" + Util.formatInt(pricePaid) + "\n" +
                            "&7Strength Bonus: &c" + bonus + "\n" +
                            "&7Damage Bonus: &c" + bonus + "\n"
            )));

            base.setHasAbility(true);

            if (!alreadyApplied) {
                base.setStrength(base.getStrength() + bonus);
                base.setDamage(base.getDamage() + bonus);
            }

            base.createStack();

            nbtItem = new NBTItem(base.getStack().clone());

            nbtItem.setBoolean("midas_sword_applied", true);

            base.setStack(nbtItem.getItem());

            return base;
        });
    }};

    public ItemHandler(Skyblock plugin) {
        this.skyblock = plugin;
    }

    public void init() {
        File folder = new File(skyblock.getDataFolder() + File.separator + "items");

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (ITEM_EXCLUSIONS.contains(file.getName().toLowerCase())) continue;

            try {
                JSONParser parser = new JSONParser();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
                Object obj = parser.parse(bufferedReader);

                JSONObject jsonObject =  (JSONObject) obj;
                String itemId = (String) jsonObject.get("itemid");
                String displayName = (String) jsonObject.get("displayname");

                String nbt = (String) jsonObject.get("nbttag");

                long damage = (long) jsonObject.get("damage");

                JSONArray lore = (JSONArray) jsonObject.get("lore");

                ItemStack item = CraftItemStack.asNewCraftStack(Item.d(itemId));
                item.setDurability((short) damage);

                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(displayName);

                //noinspection unchecked
                meta.setLore(lore);
                meta.spigot().setUnbreakable(true);

                item.setItemMeta(meta);

                net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
                nms.setTag(MojangsonParser.parse(nbt));

                NBTItem nbtItem = new NBTItem(CraftItemStack.asBukkitCopy(nms));
                nbtItem.setString("skyblockId", file.getName().replace(".json", "").toLowerCase());

                register(file.getName(), nbtItem.getItem());

                if (jsonObject.get("recipe") != null) {
                    JSONObject recipe = (JSONObject) jsonObject.get("recipe");
                    Skyblock.getPlugin(Skyblock.class).getRecipeHandler().getRecipes().add(
                            new SkyblockRecipe(recipe, getItem(file.getName())));
                }

                bufferedReader.close();
            } catch (MojangsonParseException | ParseException | IOException ex) {
                ex.printStackTrace();
                return;
            }
        }

        registerCustomItems();
    }

    public void registerCustomItems() {
        skyblock.sendMessage("Registering custom items...");

        long start = System.currentTimeMillis();

        items.put("fancy_sword", new ItemBase(
                Material.GOLD_SWORD,
                ChatColor.WHITE + "Fancy Sword",
                Reforge.NONE,
                1,
                new ArrayList<>(),
                new ArrayList<ItemEnchantment>() {{
                    add(new ItemEnchantment(skyblock.getEnchantmentHandler().getEnchantment("first_strike"), 1));
//                    add(new ItemEnchantment(skyblock.getEnchantmentHandler().getEnchantment("scavenger"), 1))
                    add(new ItemEnchantment(skyblock.getEnchantmentHandler().getEnchantment("sharpness"), 2));
//                    add(new ItemEnchantment(skyblock.getEnchantmentHandler().getEnchantment("vampirism"), 1));
                }},
                true,
                false,
                "",
                new ArrayList<>(),
                "",
                0,
                "",
                "COMMON SWORD",
                "fancy_sword",
                20,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false
        ).createStack());

        items.put("raider_axe", new ItemBase(
                Material.IRON_AXE,
                ChatColor.BLUE + "Raider Axe",
                Reforge.NONE,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                false,
                false,
                null,
                new ArrayList<>(),
                null,
                0,
                null,
                "RARE SWORD",
                "raider_axe",
                80, 50, 0, 0, 0, 0, 0, 0, 0, true,
                new HashMap<String, Object>() {{
                    put("raider_axe_kills", 0);
                    put("raider_axe_collection", 0);
                }},
                SKYBLOCK_ID_TO_ITEM.get("raider_axe")
        ).createStack());

        items.put("midas_sword", new ItemBase(
                Material.GOLD_SWORD,
                ChatColor.GOLD + "Midas' Sword",
                Reforge.NONE,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                false,
                false,
                null,
                new ArrayList<>(),
                null,
                0,
                null,
                "LEGENDARY SWORD",
                "midas_sword",
                120, 0, 0, 0, 0, 0, 0, 0, 0, false,
                new HashMap<String, Object>() {{
                    put("midas_sword_price_paid", 100000);
                    put("midas_sword_applied", false);
                }},
                SKYBLOCK_ID_TO_ITEM.get("midas_sword")
        ).createStack());

        for (Rarity r : Rarity.values()) {
            for (PetType type : PetType.values()) {
                Pet pet = type.newInstance();
                if (pet == null) continue;
                pet.setRarity(r);
                items.put(type.name(), pet.toItemStack());
            }

            if (r.equals(Rarity.LEGENDARY)) break;
        }

        SkyblockEnchantmentHandler enchantmentHandler = skyblock.getEnchantmentHandler();

        for (SkyblockEnchantment enchant : enchantmentHandler.getEnchantments()) {
            for (int i = 1; i <= enchant.getMaxLevel(); i++) {
                items.put(enchant.getName().toUpperCase() + ";" + i + ".json",
                        new ItemBuilder(ChatColor.WHITE + "Enchanted Book", Material.ENCHANTED_BOOK)
                                .addLore(ChatColor.BLUE + enchant.getDisplayName() + " " + Util.toRoman(i)).addLore(Util.enchantLore(enchant.getDescription(i)))
                                .addLore(" ", "&7Use this on an item in an Anvil", "&7to apply it!", " ", ChatColor.WHITE + "" + ChatColor.BOLD + "COMMON").toItemStack());
            }
        }

        skyblock.sendMessage("Sucessfully Registered custom items [" + Util.getTimeDifferenceAndColor(start, System.currentTimeMillis()) + ChatColor.WHITE + "]");
    }

    public void register(String id, ItemStack item) {
        items.put(id, parseLore(item));
        reversed.put(parseLore(item), id.replace(".json", ""));
    }

    public ItemStack getItem(String s) {
        return items.get(s);
    }

    private ItemStack parseLore(ItemStack item) {
        NBTItem nbt = new NBTItem(item);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        nbt.setBoolean("skyblockItem", true);
        nbt.setString("name", meta.getDisplayName());
        nbt.setString("reforge", "NO_REFORGE");

        if (item.getEnchantments().size() > 0) {
            nbt.setBoolean("enchantGlint", true);
        }

        boolean passedAbility = false;
        boolean finishedAbilityLore = false;

        for (String string : lore) {
            if (string.toLowerCase().contains("this item can be reforged")) nbt.setBoolean("reforgeable", true);
        }

        for (int i = 0; i < lore.size(); i++) {
            String string = lore.get(i);

            List<String> list = Arrays.asList(ChatColor.stripColor(string.toLowerCase()).replaceAll("%", "").replaceAll("hp", "").replaceAll(" ", "").split(":+"));
            String line = ChatColor.stripColor(string.toLowerCase());

            try {
                int value = Integer.parseInt(list.get(1));

                if (line.startsWith("strength")) {
                    nbt.setInteger("strength", value);
                } else if (line.startsWith("health")) {
                    nbt.setInteger("health", value);
                } else if (line.startsWith("damage")) {
                    nbt.setInteger("damage", value);
                } else if (line.startsWith("crit chance")) {
                    nbt.setInteger("critChance", value);
                } else if (line.startsWith("crit damage")) {
                    nbt.setInteger("critDamage", value);
                } else if (line.startsWith("defense")) {
                    nbt.setInteger("defense", value);
                } else if (line.startsWith("true defense")) {
                    nbt.setInteger("trueDefense", value);
                } else if (line.startsWith("speed")) {
                    nbt.setInteger("speed", value);
                } else if (line.startsWith("intelligence")) {
                    nbt.setInteger("intelligence", value);
                } else if (line.startsWith("magic find")) {
                    nbt.setInteger("magicFind", value);
                } else if (line.startsWith("sea creature chance")) {
                    nbt.setInteger("seaCreatureChance", value);
                } else if (line.startsWith("pet luck")) {
                    nbt.setInteger("petLuck", value);
                } else if (line.startsWith("ferocity")) {
                    nbt.setInteger("ferocity", value);
                } else if (line.startsWith("ability damage")) {
                    nbt.setInteger("abilityDamage", value);
                } else if (line.startsWith("mana cost: ")) {
                    nbt.setInteger("abilityCost", value);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                if (line.startsWith("mana cost: ")) {
                    nbt.setInteger("abilityCost", Integer.parseInt(line.substring(11)));
                    continue;
                } else if (line.startsWith("item ability: ")) {
                    nbt.setBoolean("hasAbility", true);

                    String sub = line.substring(14);
                    String abilityName = WordUtils.capitalize(sub);
                    String type = "";

                    if (sub.contains("right click")) {
                        type = "Right Click";
                    } else if (sub.contains("left click")) {
                        type = "Left Click";
                    }

                    nbt.setString("abilityName", abilityName.replaceAll(type, ""));
                    nbt.setString("abilityType", type.toUpperCase());
                    passedAbility = true;
                    continue;
                } else if (line.startsWith("cooldown: ")) {
                    nbt.setString("abilityCooldown", line.substring(10));
                    continue;
                } else if (passedAbility && !finishedAbilityLore) {
                    List<String> ability = new ArrayList<>();
                    for (int j = i; j < lore.size(); j++) {
                        String raw = ChatColor.stripColor(lore.get(j)).toLowerCase();
                        if (raw.startsWith("this item can be reforged!") ||
                                raw.startsWith("cooldown: ") ||
                                raw.startsWith("mana cost: ") ||
                            j == lore.size() - 1){
                            i = j-1;
                            break;
                        }

                        ability.add(lore.get(j));
                    }

                    nbt.setString("abilityDescription", ability.toString());
                    finishedAbilityLore = true;
                    continue;
                } else if (!line.isEmpty()) {
                    List<String> desc = new ArrayList<>();
                    for (int j = i; j < lore.size(); j++) {
                        String raw = ChatColor.stripColor(lore.get(j)).toLowerCase();
                        if (raw.startsWith("this item can be reforged!") ||
                            raw.startsWith("item ability: ") ||
                            j == lore.size() - 1){
                            i = j;
                            break;
                        }

                        desc.add(lore.get(j));
                    }

                    if (desc.size() > 1) {
                        nbt.setString("description", desc.toString());
                    }
                }

                if (i == lore.size() - 1) {
                    nbt.setString("rarity", lore.get(i));
                }
            }
        }

        if (!nbt.hasKey("description")) {
            nbt.setString("description", "placeholder description");
        }

        if (!passedAbility) {
            nbt.setString("abilityDescription", "placeholder description");
        }

        if (!nbt.hasKey("enchantments")) {
            nbt.setString("enchantments", "[]");
        }

        ItemBase base = new ItemBase(nbt.getItem());

        return base.getStack();
    }
}
