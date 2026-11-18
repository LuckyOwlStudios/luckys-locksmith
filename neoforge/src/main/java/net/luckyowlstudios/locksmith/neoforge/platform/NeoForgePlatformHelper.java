package net.luckyowlstudios.locksmith.neoforge.platform;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.neoforge.compat.CuriosCompat;
import net.luckyowlstudios.locksmith.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class NeoForgePlatformHelper implements IPlatformHelper {

    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    /** Hooks every deferred register created through {@link #register} up to the mod event bus. */
    public static void registerAll(IEventBus modEventBus) {
        REGISTERS.values().forEach(register -> register.register(modEventBus));
    }

    private static boolean isCuriosLoaded() {
        return ModList.get().isLoaded("curios");
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) REGISTERS.computeIfAbsent(registry.key(),
                key -> DeferredRegister.create(registry.key(), Locksmith.MOD_ID));
        return register.register(name, factory);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public KeyItem createKeyItem(Item.Properties properties) {
        return isCuriosLoaded() ? CuriosCompat.createKeyItem(properties) : new KeyItem(properties);
    }

    @Override
    public Optional<ItemStack> findEquippedKey(Player player) {
        return isCuriosLoaded() ? CuriosCompat.findEquippedKey(player) : Optional.empty();
    }
}
