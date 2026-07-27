package cnnr.groundwork.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Adds a bob-animation time sample, the wisp's region bounds, and the local player's eye/look
 *  (for face targeting) to the base render state. regionMin/Max/eye/look are null when unset. */
public class WispRenderState extends BlockEntityRenderState {
    public float time;
    public BlockPos regionMin;
    public BlockPos regionMax;
    public Vec3 eye;
    public Vec3 look;
}
