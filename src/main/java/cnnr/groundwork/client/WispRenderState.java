package cnnr.groundwork.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Adds a bob-animation time sample to the base render state (position/lightCoords already provided by the base). */
public class WispRenderState extends BlockEntityRenderState {
    public float time;
}
