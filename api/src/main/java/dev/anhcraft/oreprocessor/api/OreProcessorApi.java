package dev.anhcraft.oreprocessor.api;

import dev.anhcraft.oreprocessor.api.data.PlayerData;
import dev.anhcraft.oreprocessor.api.data.ServerData;
import dev.anhcraft.oreprocessor.api.integration.ShopProviderType;
import dev.anhcraft.oreprocessor.api.upgrade.UpgradeLevel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public interface OreProcessorApi {
    /**
     * Gets all registered ore IDs.
     * @return All ore IDs
     */
    @NotNull
    Set<String> getOres();

    /**
     * Gets the ore with given ID
     * @param id The ore ID
     * @return The ore
     */
    @Nullable
    Ore getOre(@NotNull String id);

    /**
     * Requires an ore with the given ID.
     * @param id The ore ID
     * @return The ore
     * @throws NullPointerException if no ore found
     */
    @NotNull
    default Ore requireOre(@NotNull String id) {
        return Objects.requireNonNull(getOre(id), "Ore not found: " + id);
    }

    /**
     * Gets the ore which is allowed for the given block material.
     * @param block The block material
     * @return The ore
     */
    @Nullable
    Ore getBlockOre(Material block);

    /**
     * Gets all ores that accept the given feedstock.
     * @param feedstock The feedstock
     * @return Found ores
     */
    @NotNull
    Collection<Ore> getOresAllowFeedstock(Material feedstock);

    /**
     * Gets the global processing speed.
     * @return The global processing speed
     */
    int getProcessingSpeed();

    /**
     * Gets the default capacity.
     * @return The default capacity
     */
    int getDefaultCapacity();

    /**
     * Gets the default throughput.
     * @return The default throughput
     */
    int getDefaultThroughput();

    /**
     * Gets throughput per minute based on current processing speed.
     * @param throughput The current throughput
     * @return The throughput per minute
     */
    int getThroughputPerMinute(int throughput);

    /**
     * Gets player data of an online player.<br>
     * For any online player, the data always exists during their session.
     * @param player The player
     * @return The player data
     */
    @NotNull
    PlayerData getPlayerData(@NotNull Player player);

    /**
     * Gets player data of a player from the cache without fetching or blocking.<br>
     * If the player is online, the data always exists. If the player is offline and his data was recently fetched,
     * then this returns immediately. Otherwise, this returns {@code null}.<br>
     * To attempt to fetch the data of any player, use {@link #requirePlayerData(UUID)}.
     * @param id The player ID
     * @return The player data
     */
    @NotNull
    Optional<PlayerData> getPlayerData(@NotNull UUID id);

    /**
     * Requires the player data from the cache or fetches if not exists yet.<br>
     * This returns a {@link CompletableFuture} which will be done asynchronously. For online player and offline player
     * whose data exists in cache, the result is immediately returned. Otherwise, it will take some time.
     * @param id The player ID
     * @return The player data
     */
    @NotNull
    CompletableFuture<PlayerData> requirePlayerData(@NotNull UUID id);

    /**
     * Gets the server data.
     * @return The server data
     */
    @NotNull
    ServerData getServerData();

    /**
     * Gets the next throughput upgrade.
     * @param currentThroughput The current throughput
     * @return The next throughput upgrade
     */
    @Nullable
    UpgradeLevel getNextThroughputUpgrade(int currentThroughput);

    /**
     * Gets the next capacity upgrade.
     * @param currentCapacity The current capacity
     * @return The next capacity upgrade
     */
    @Nullable
    UpgradeLevel getNextCapacityUpgrade(int currentCapacity);

    /**
     * Gets the shop provider.
     * @return The shop provider
     */
    ShopProviderType getShopProvider();
}
