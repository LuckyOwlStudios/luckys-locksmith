package net.luckyowlstudios.locksmith.util;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** Transform from the north-facing, closed panel to the door's current panel. */
public final class DoorLockPlacement {
    private DoorLockPlacement() {}

    public static float rotation(BlockState state) {
        float rotation = 180 - state.getValue(DoorBlock.FACING).toYRot();
        if (state.getValue(DoorBlock.OPEN)) {
            rotation += state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? 90 : -90;
        }
        return rotation;
    }

    public static double handleX(BlockState state) {
        return state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? 0.22 : 0.78;
    }
}
