package net.luckyowlstudios.locksmith.fabric.platform;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        T value = Registry.register(registry, Locksmith.id(name), factory.get());
        if (value instanceof BlockItem blockItem) {
            // Vanilla only fills this map for items registered through Items.registerItem
            blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
        }
        return () -> value;
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }

    @Override
    public Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Item.Properties properties) {
        // Fabric registers immediately and entity types are registered before items, so the type exists
        return new SpawnEggItem(type.get(), backgroundColor, highlightColor, properties);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public KeyItem createKeyItem(Item.Properties properties) {
        return new KeyItem(properties);
    }

    @Override
    public Optional<ItemStack> findEquippedKey(Player player) {
        // No accessory mod support on Fabric yet
        return Optional.empty();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
