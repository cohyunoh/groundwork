package cnnr.groundwork.client;

import cnnr.groundwork.block.WispBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Read-mode wisp visuals. Default: every wisp shows only its bobbing marker (no region box --
 * that would be clutter). Selecting a wisp (right-click with the Lens in Builder Vision) focuses
 * it: its marker emphasizes, its region box appears, and every OTHER wisp's marker is hidden.
 */
public class WispBlockEntityRenderer implements BlockEntityRenderer<WispBlockEntity, WispRenderState> {
    private static final int MARKER_COLOR = 0xFFCC66FF;
    private static final int SELECTED_MARKER_COLOR = 0xFFFFCC33;
    private static final int REGION_COLOR = 0x8866CCFF;
    private static final int HIGHLIGHT_COLOR = 0xCCFFDD33;
    private static final double AIM_MAX_DIST = 64.0;
    private static final double FACE_THICKNESS = 0.02;

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

        LocalPlayer player = mc.player;
        if (player != null) {
            state.eye = player.getEyePosition(partialTick);
            state.look = player.getViewVector(partialTick);
        } else {
            state.eye = null;
            state.look = null;
        }
    }

    @Override
    public void submit(WispRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!ClientBuildVision.isActive()) return;

        BlockPos origin = state.blockPos;
        BlockPos selected = ClientWispSelection.getSelected();
        boolean thisSelected = origin.equals(selected);
        boolean somethingElseSelected = selected != null && !thisSelected;
        if (somethingElseSelected) return; // focus: hide every wisp except the selected one

        float bob = Mth.sin(state.time * 0.1f) * 0.08f;
        double y = 1.0 + bob;
        double half = thisSelected ? 0.2 : 0.15;
        double top = thisSelected ? 0.4 : 0.3;
        double lift = thisSelected ? 0.15 : 0.0;
        int markerColor = thisSelected ? SELECTED_MARKER_COLOR : MARKER_COLOR;
        float markerWidth = thisSelected ? 3.0f : 2.0f;
        VoxelShape marker = Shapes.box(0.5 - half, y + lift, 0.5 - half, 0.5 + half, y + lift + top, 0.5 + half);
        collector.submitShapeOutline(pose, marker, RenderTypes.lines(), markerColor, markerWidth, false);

        if (!thisSelected) return; // region box (and face highlight) only for the focused wisp
        if (state.regionMin == null || state.regionMax == null) return;

        double x0 = state.regionMin.getX() - origin.getX();
        double y0 = state.regionMin.getY() - origin.getY();
        double z0 = state.regionMin.getZ() - origin.getZ();
        double x1 = state.regionMax.getX() - origin.getX() + 1.0;
        double y1 = state.regionMax.getY() - origin.getY() + 1.0;
        double z1 = state.regionMax.getZ() - origin.getZ() + 1.0;
        VoxelShape regionBox = Shapes.box(x0, y0, z0, x1, y1, z1);
        collector.submitShapeOutline(pose, regionBox, RenderTypes.lines(), REGION_COLOR, 2.0f, false);

        boolean editingThisWisp = ClientRegionEdit.isEditing() && origin.equals(ClientRegionEdit.getWispPos());
        if (!editingThisWisp || state.eye == null || state.look == null) return;

        AABB worldBox = new AABB(state.regionMin.getX(), state.regionMin.getY(), state.regionMin.getZ(),
                                  state.regionMax.getX() + 1.0, state.regionMax.getY() + 1.0, state.regionMax.getZ() + 1.0);
        Direction face = RegionFaceTargeter.aimedFace(state.eye, state.look, worldBox, AIM_MAX_DIST);
        if (face == null) return;

        VoxelShape faceShape = faceHighlightShape(face, x0, y0, z0, x1, y1, z1);
        collector.submitShapeOutline(pose, faceShape, RenderTypes.lines(), HIGHLIGHT_COLOR, 4.0f, false);
    }

    /** A thin slab hugging the given face of the render-local box, for highlighting that face's outline. */
    private static VoxelShape faceHighlightShape(Direction face, double x0, double y0, double z0,
                                                  double x1, double y1, double z1) {
        return switch (face) {
            case WEST -> Shapes.box(x0, y0, z0, x0 + FACE_THICKNESS, y1, z1);
            case EAST -> Shapes.box(x1 - FACE_THICKNESS, y0, z0, x1, y1, z1);
            case DOWN -> Shapes.box(x0, y0, z0, x1, y0 + FACE_THICKNESS, z1);
            case UP -> Shapes.box(x0, y1 - FACE_THICKNESS, z0, x1, y1, z1);
            case NORTH -> Shapes.box(x0, y0, z0, x1, y1, z0 + FACE_THICKNESS);
            case SOUTH -> Shapes.box(x0, y0, z1 - FACE_THICKNESS, x1, y1, z1);
        };
    }
}
