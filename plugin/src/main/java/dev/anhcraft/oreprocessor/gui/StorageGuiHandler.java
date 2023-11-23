package dev.anhcraft.oreprocessor.gui;

import dev.anhcraft.config.bukkit.utils.ItemBuilder;
import dev.anhcraft.oreprocessor.OreProcessor;
import dev.anhcraft.oreprocessor.api.Ore;
import dev.anhcraft.oreprocessor.api.data.OreData;
import dev.anhcraft.oreprocessor.api.data.PlayerData;
import dev.anhcraft.palette.event.ClickEvent;
import dev.anhcraft.palette.ui.GuiHandler;
import dev.anhcraft.palette.util.ItemReplacer;
import dev.anhcraft.palette.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StorageGuiHandler extends GuiHandler implements AutoRefresh {
    private final static NumberFormat numberFormat = NumberFormat.getInstance();
    private final OreProcessor plugin;
    private final String oreId;
    private final Ore ore;
    private OreData oreData;

    public StorageGuiHandler(String oreId) {
        this.plugin = OreProcessor.getInstance();
        this.oreId = oreId;
        this.ore = OreProcessor.getApi().requireOre(oreId);
    }

    @Override
    public void onPreOpen(@NotNull Player player) {
        PlayerData playerData = OreProcessor.getApi().getPlayerData(player);
        this.oreData = playerData.requireOreData(oreId);

        listen("ore", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                GuiRegistry.openUpgradeGui(player, oreId);
            }
        });

        listen("back", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                GuiRegistry.openMenuGui(player);
            }
        });

        listen("craft", new ClickEvent() {
            @Override
            public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int slot) {
                //
            }
        });

        refresh(player);
    }

    @Override
    public void refresh(Player player) {
        int stored = oreData.countAllProducts();
        int cap = oreData.getCapacity();
        int processing = oreData.countAllFeedstock();
        int throughputM = OreProcessor.getApi().getThroughputPerMinute(oreData.getThroughput());

        replaceItem("ore", new ItemReplacer() {
            @Override
            public @NotNull ItemBuilder apply(int slot, @NotNull ItemBuilder itemBuilder) {
                itemBuilder.material(ore.getIcon());
                itemBuilder.replaceDisplay(s -> s.replace("{ore}", ore.getName())
                        .replace("{processing}", Integer.toString(processing))
                        .replace("{storage-current}", Integer.toString(stored))
                        .replace("{storage-capacity}", Integer.toString(cap))
                        .replace("{storage-ratio}", Integer.toString((int) (((double) stored) / cap * 100d)))
                        .replace("{throughput}", Integer.toString(throughputM)));
                return itemBuilder;
            }
        });

        List<Integer> productSlots = new ArrayList<>(locateComponent("product"));
        Collections.sort(productSlots);
        List<Material> products = new ArrayList<>(oreData.getProducts());
        if (products.size() > productSlots.size()) {
            plugin.getLogger().warning(String.format(
                    "%s has %d products while GUI's display capability is %d (ore=%s)",
                    player.getName(), products.size(), productSlots.size(), ore.getName()
            ));
        }

        for (int i = 0; i < productSlots.size(); i++) {
            int slot = productSlots.get(i);

            if (i >= products.size()) {
                resetItem(slot);

                getSlot(slot).setEvents((ClickEvent) (e, p, i1) -> handleAddProduct(p, p.getItemOnCursor(), products));
                continue;
            }

            Material product = products.get(i);
            ItemBuilder itemBuilder = GuiRegistry.STORAGE.getProductIcon();
            itemBuilder.material(product);

            itemBuilder.replaceDisplay(s -> s.replace("{current}", Integer.toString(oreData.countProduct(product))));

            getInventory().setItem(slot, itemBuilder.build());
            getSlot(slot).setEvents(new ClickEvent() {
                @Override
                public void onClick(@NotNull InventoryClickEvent clickEvent, @NotNull Player player, int i) {
                    ItemStack cursor = player.getItemOnCursor();

                    // TAKE
                    if (ItemUtil.isEmpty(cursor)) {
                        Integer many = plugin.mainConfig.accessibilitySettings.takeAmount.get(clickEvent.getClick());
                        if (many == null || many <= 0) return;
                        int oldTotalAmount = oreData.countProduct(product);
                        int actual = oreData.takeProduct(product, many);
                        if (actual == 0) {
                            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                            return;
                        }
                        ItemUtil.addToInventory(player, new ItemStack(product, actual));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1f, 1f);
                        plugin.pluginLogger.scope("storage")
                                .add("player", player)
                                .add("ore", ore)
                                .add("product", product)
                                .add("action", "take")
                                .add("bulkSize", many)
                                .add("delta", actual)
                                .add("oldTotalAmount", oldTotalAmount)
                                .add("newTotalAmount", oldTotalAmount - actual)
                                .flush();
                        return;
                    }

                    // ADD
                    handleAddProduct(player, cursor, products);
                }
            });
        }

        resetBulk("quick-sell");

        //
    }

    private void handleAddProduct(Player player, ItemStack cursor, Collection<Material> products) {
        if (ItemUtil.isEmpty(cursor)) return;
        Material material = cursor.getType();

        if (oreData.isFull()) {
            plugin.msg(player, plugin.messageConfig.storageFull);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        } else if (!cursor.hasItemMeta() && (products.contains(material) ||
                ore.getAllowedProducts().contains(material) ||
                ore.getBestTransform(player).hasProduct(material))) {
            int oldTotalAmount = oreData.countProduct(material);
            int stored = oreData.addProduct(material, cursor.getAmount(), false);
            int remain = cursor.getAmount() - stored;
            if (remain == 0) {
                player.setItemOnCursor(null);
            } else {
                ItemStack clone = cursor.clone();
                clone.setAmount(remain);
                player.setItemOnCursor(clone);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1f, 1f);
            plugin.pluginLogger.scope("storage")
                    .add("player", player)
                    .add("ore", ore)
                    .add("product", material)
                    .add("action", "add")
                    .add("expectedDelta", cursor.getAmount())
                    .add("actualDelta", stored)
                    .add("remainDelta", remain)
                    .add("oldTotalAmount", oldTotalAmount)
                    .add("newTotalAmount", oldTotalAmount + stored)
                    .flush();
        } else {
            plugin.msg(player, plugin.messageConfig.storeInvalidItem);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        }
    }
}
