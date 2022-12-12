package com.skyblock.skyblock.features.bazaar.impl;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.enums.Rarity;
import com.skyblock.skyblock.features.bazaar.*;
import com.skyblock.skyblock.features.bazaar.escrow.Escrow;
import com.skyblock.skyblock.utilities.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SkyblockBazaar implements Bazaar {

    private final Escrow escrow;
    private final List<BazaarCategory> categories;

    private final List<BazaarSubItem> rawItems;

    private final YamlConfiguration config;
    private final File file;

    public SkyblockBazaar() throws BazaarIOException, BazaarItemNotFoundException {
        this.escrow = null;
        this.categories = new ArrayList<>();
        this.rawItems = new ArrayList<>();

        this.file = new File(Skyblock.getPlugin().getDataFolder(), Bazaar.FILE_NAME);

        this.config = YamlConfiguration.loadConfiguration(this.file);

        // todo: add categories and raw items

        // temp:
        this.rawItems.add(new SkyblockBazaarSubItem(Skyblock.getPlugin().getItemHandler().getItem("ENCHANTED_PUMPKIN.json"), Rarity.UNCOMMON, 12, new ArrayList<BazaarOffer>() {{
            add(new SkyblockBazaarOffer(UUID.randomUUID(), 1349, 824.3));
        }}, new ArrayList<>()));

        this.categories.add(new SkyblockBazaarCategory("Farming", Material.GOLD_HOE, ChatColor.YELLOW, (short) 4, new ArrayList<BazaarItem>() {{
            add(new SkyblockBazaarItem("Pumpkin", new ArrayList<BazaarSubItem>() {{
                add(rawItems.get(0));
            }}));
        }}));

        if (!this.file.exists()) {
            try {
                boolean success = this.file.createNewFile();

                if (!success) throw new IOException("File#createNewFile returned false");
            } catch (IOException ex) {
                throw new BazaarIOException("Failed to create " + file.getAbsolutePath() + ", please check your file permissions.", ex);
            }

            this.set("items", new ArrayList<>());

            this.rawItems.forEach(item -> {
                try {
                    String name = ChatColor.stripColor(item.getIcon().getItemMeta().getDisplayName()).toUpperCase().replace(" ", "_");
                    this.set("items." + name + ".buyPrice", 0.0);
                    this.set("items." + name + ".sellPrice", 0.0);
                    this.set("items." + name + ".orders", item.getOrders());
                    this.set("items." + name + ".offers", item.getOffers());
                    this.set("items." + name + ".buyVolume.amount", 0);
                    this.set("items." + name + ".buyVolume.offers", 0);
                    this.set("items." + name + ".last7dInstantBuyVolume", 0);
                    this.set("items." + name + ".sellVolume.amount", 0);
                    this.set("items." + name + ".sellVolume.orders", 0);
                    this.set("items." + name + ".last7dInstantSellVolume", 0);
                } catch (BazaarIOException ex) {
                    ex.printStackTrace();
                }
            });

            this.categories.forEach(category -> {
                try {
                    this.set("categories." + category.getName() + ".icon", category.getIcon());
                    this.set("categories." + category.getName() + ".items", new ArrayList<>());

                    for (BazaarItem item : category.getItems()) {
                        this.set("categories." + category.getName() + ".items." + item.getName(), new ArrayList<>());

                        for (BazaarSubItem subItem : item.getSubItems()) {
                            this.set("categories." + category.getName() + ".items." + item.getName() + "." + ChatColor.stripColor(subItem.getIcon().getItemMeta().getDisplayName()).toUpperCase().replace(" ", "_"), subItem.getSlot());
                        }
                    }
                } catch (BazaarIOException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    @Override
    public YamlConfiguration getBazaarConfig() {
        return this.config;
    }

    @Override
    public Escrow getEscrow() {
        return this.escrow;
    }

    @Override
    public List<BazaarCategory> getCategories() {
        return this.categories;
    }

    @Override
    public BazaarItemData getItemData(String name) throws BazaarItemNotFoundException {
        AtomicReference<SkyblockBazaarItemData> data = new AtomicReference<>();

        this.config.getConfigurationSection("items").getKeys(false).forEach(key -> {
            if (key.equalsIgnoreCase(name)) {
                data.set(new SkyblockBazaarItemData(
                        this.get("items." + key + ".productAmount", Integer.class),
                        this.get("items." + key + ".buyPrice", Double.class),
                        this.get("items." + key + ".sellPrice", Double.class),
                        new SkyblockBazaarItemData.SkyblockBazaarItemVolume(
                                this.get("items." + key + ".buyVolume.amount", Integer.class),
                                this.get("items." + key + ".buyVolume.offers", Integer.class)
                        ),
                        this.get("items." + key + ".last7dInstantBuyVolume", Integer.class),
                        new SkyblockBazaarItemData.SkyblockBazaarItemVolume(
                                this.get("items." + key + ".sellVolume.amount", Integer.class),
                                this.get("items." + key + ".sellVolume.orders", Integer.class)
                        ),
                        this.get("items." + key + ".last7dInstantSellVolume", Integer.class),
                        this.get("items." + key + ".orders", List.class),
                        this.get("items." + key + ".offers", List.class)
                ));
            }
        });

        if (data.get() == null) throw new BazaarItemNotFoundException("Item " + name + " not found in bazaar config");

        return data.get();
    }

    @Override
    public void set(String path, Object value) throws BazaarIOException{
        try {
            this.config.set(path, value);
            this.config.save(this.file);
        } catch (IOException ex) {
            throw new BazaarIOException("Failed to save bazaar config", ex);
        }
    }

    @Override
    public Object get(String path) {
        return this.config.get(path);
    }

    public <T> T get(String path, Class<T> clazz) {
        return clazz.cast(this.get(path));
    }

}
