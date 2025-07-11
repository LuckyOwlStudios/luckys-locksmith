package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Locksmith.MOD_ID);

        public static final Supplier<CreativeModeTab> SPELUNKERS_CHARM = CREATIVE_MODE_TAB.register("locksmith",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.KEY.get()))
                    .title(Component.translatable("itemGroup.locksmith"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.KEY.get().getDefaultInstance());
                        output.accept(ModItems.GOLDEN_KEY.get().getDefaultInstance());
                        output.accept(Items.TRIAL_KEY);
                        output.accept(ModItems.GOLDEN_LOCK.get().getDefaultInstance());
                        output.accept(ModItems.TRIAL_LOCK.get().getDefaultInstance());
                        output.accept(Items.CHEST);
                        output.accept(ModBlocks.IRON_CHEST.toStack());
                        output.accept(ModBlocks.GOLDEN_CHEST.toStack());
                        output.accept(Items.TRAPPED_CHEST);
                        output.accept(ModBlocks.IRON_TRAPPED_CHEST.toStack());
                        output.accept(ModBlocks.GOLDEN_TRAPPED_CHEST.toStack());
                        output.accept(Items.BARREL);
                    })
                    .build());

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS || event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.insertAfter(Items.CHEST.getDefaultInstance(), ModBlocks.IRON_CHEST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.IRON_CHEST.toStack(), ModBlocks.GOLDEN_CHEST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.insertAfter(Items.TRAPPED_CHEST.getDefaultInstance(), ModBlocks.IRON_TRAPPED_CHEST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.IRON_TRAPPED_CHEST.toStack(), ModBlocks.GOLDEN_TRAPPED_CHEST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.insertAfter(Items.EXPERIENCE_BOTTLE.getDefaultInstance(), ModItems.KEY.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.KEY.toStack(), ModItems.GOLDEN_KEY.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(ModItems.GOLDEN_LOCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.TRIAL_LOCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
