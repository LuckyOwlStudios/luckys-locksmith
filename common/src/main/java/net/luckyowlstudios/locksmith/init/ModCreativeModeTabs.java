package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * The mod's own creative tab. Adding items to vanilla tabs is loader specific and lives in each
 * loader module.
 */
public class ModCreativeModeTabs {

    public static final Supplier<CreativeModeTab> LOCKSMITH = Services.PLATFORM.register(BuiltInRegistries.CREATIVE_MODE_TAB, "locksmith",
            () -> Services.PLATFORM.creativeTabBuilder()
                    .icon(() -> new ItemStack(ModItems.KEY.get()))
                    .title(Component.translatable("itemGroup.locksmith"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.KEY.get().getDefaultInstance());
                        output.accept(ModItems.GOLDEN_KEY.get().getDefaultInstance());
                        output.accept(Items.TRIAL_KEY);
                        output.accept(ModItems.GOLDEN_LOCK.get().getDefaultInstance());
                        output.accept(ModItems.TRIAL_LOCK.get().getDefaultInstance());
                        output.accept(Items.CHEST);
                        output.accept(ModBlocks.IRON_CHEST.get());
                        output.accept(ModBlocks.GOLDEN_CHEST.get());
                        output.accept(Items.TRAPPED_CHEST);
                        output.accept(ModBlocks.IRON_TRAPPED_CHEST.get());
                        output.accept(ModBlocks.GOLDEN_TRAPPED_CHEST.get());
                        output.accept(Items.BARREL);
                        output.accept(ModItems.DUNGEON_SLIME_SPAWN_EGG.get());
                    })
                    .build());

    public static void init() {
    }
}
