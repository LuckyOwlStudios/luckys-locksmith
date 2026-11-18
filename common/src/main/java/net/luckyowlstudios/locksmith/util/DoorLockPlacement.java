package net.luckyowlstudios.locksmith.util;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Where the padlock goes. It is worked out for a closed door facing north, whose 3-pixel panel is at
 * z 0.8125 to 1 and spans x 0 to 1: {@link #rotation} turns that to the door's facing, and an open door
 * additionally swings {@link #openAngle} around its hinge, {@link #HINGE_PIVOT_Z} deep in the panel and
 * {@link #hingePivotX} in from the hinge edge, exactly where vanilla's open door model ends up.
 */
public final class DoorLockPlacement {
    /** The middle of the panel's thickness, which the open door swings around. */
    public static final double HINGE_PIVOT_Z = 1 - 1.5 / 16.0;

    private DoorLockPlacement() {}

    /** Turns the north-facing closed door to the door's facing, around the middle of the block. */
    public static float rotation(BlockState state) {
        return 180 - state.getValue(DoorBlock.FACING).toYRot();
    }

    /** How far the open door has swung around its hinge, in degrees; 0 when it is closed. */
    public static float openAngle(BlockState state) {
        if (!state.getValue(DoorBlock.OPEN)) return 0;
        return state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? -90 : 90;
    }

    /** The x of the hinge pivot of the north-facing door: the hinge edge, less half the panel's thickness. */
    public static double hingePivotX(BlockState state) {
        return state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? 1 - 1.5 / 16.0 : 1.5 / 16.0;
    }

    public static double handleX(BlockState state) {
        return state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? 0.22 : 0.78;
    }
}
