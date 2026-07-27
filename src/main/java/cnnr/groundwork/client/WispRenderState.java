package cnnr.groundwork.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

/** Adds a bob-animation time sample and the wisp's region bounds to the base render state
 *  (position/lightCoords already provided by the base). regionMin/Max are null when unset. */
public class WispRenderState extends BlockEntityRenderState {
    public float time;
    public BlockPos regionMin;
    public BlockPos regionMax;
}
