package cnnr.groundwork.client;

import cnnr.groundwork.block.WispBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Placeholder marker: a small bobbing wireframe box hovering above the wisp, visible only in Builder Vision. */
public class WispBlockEntityRenderer implements BlockEntityRenderer<WispBlockEntity, WispRenderState> {
    private static final int MARKER_COLOR = 0xFFCC66FF;
    private static final int REGION_COLOR = 0x8866CCFF;

    public WispBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public WispRenderState createRenderState() { return new WispRenderState(); }

    @Override
    public void extractRenderState(WispBlockEntity be, WispRenderState state, float partialTick, Vec3 cameraPos,
                                    ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(be, state, overlay);
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        state.time = gameTime + partialTick;

        if (be.hasRegion()) {
            state.regionMin = be.getRegion().min();
            state.regionMax = be.getRegion().max();
        } else {
            state.regionMin = null;
            state.regionMax = null;
        }
    }

    @Override
    public void submit(WispRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!ClientBuildVision.isActive()) return;

        float bob = Mth.sin(state.time * 0.1f) * 0.08f;
        double y = 1.0 + bob;
        VoxelShape marker = Shapes.box(0.35, y, 0.35, 0.65, y + 0.3, 0.65);
        collector.submitShapeOutline(pose, marker, RenderTypes.lines(), MARKER_COLOR, 2.0f, false);

        if (state.regionMin != null && state.regionMax != null) {
            BlockPos origin = state.blockPos;
            double x0 = state.regionMin.getX() - origin.getX();
            double y0 = state.regionMin.getY() - origin.getY();
            double z0 = state.regionMin.getZ() - origin.getZ();
            double x1 = state.regionMax.getX() - origin.getX() + 1.0;
            double y1 = state.regionMax.getY() - origin.getY() + 1.0;
            double z1 = state.regionMax.getZ() - origin.getZ() + 1.0;
            VoxelShape regionBox = Shapes.box(x0, y0, z0, x1, y1, z1);
            collector.submitShapeOutline(pose, regionBox, RenderTypes.lines(), REGION_COLOR, 2.0f, false);
        }
    }
}
