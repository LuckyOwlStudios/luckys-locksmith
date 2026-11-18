package net.luckyowlstudios.locksmith.platform.services;

import net.luckyowlstudios.locksmith.item.KeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Everything the common code needs from the mod loader.
 */
public interface IPlatformHelper {

    /** The name of the loader this build is running on, e.g. "NeoForge" or "Fabric". */
    String getPlatformName();

    /**
     * Registers an object under this mod's namespace. On NeoForge registration is deferred until the
     * registry event fires, so only call {@link Supplier#get()} on the result after registration.
     */
    <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory);

    /** Vanilla's block entity type builder takes a package-private factory type, so each loader builds it. */
    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks);

    /** A spawn egg; NeoForge needs its deferred spawn egg because entity types register after items there. */
    Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Item.Properties properties);

    /** A creative tab builder; vanilla's no-arg builder only exists on NeoForge. */
    CreativeModeTab.Builder creativeTabBuilder();

    /** Creates the key item, letting a loader attach accessory-slot support (Curios on NeoForge). */
    KeyItem createKeyItem(Item.Properties properties);

    /** A key the player wears in an accessory slot, if the loader has an accessory mod installed. */
    Optional<ItemStack> findEquippedKey(Player player);

    /** Sends one of this mod's payloads (registered by each loader) to a player. */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
