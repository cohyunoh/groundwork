package cnnr.groundwork.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An invisible, non-colliding marker block; all visuals come from the client-side block-entity renderer. */
public class WispBlock extends BaseEntityBlock {
    public static final MapCodec<WispBlock> CODEC = simpleCodec(WispBlock::new);

    public WispBlock(Properties props) { super(props); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WispBlockEntity(pos, state);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    // Deliberately NOT overriding getShape(): it must stay the default full cube (Shapes.block()),
    // since that's the shape the crosshair raycast (ClipContext.Block.OUTLINE, used for both
    // targeting and breaking) hit-tests against. Emptying it would make the wisp unclickable.
    // The vanilla debug outline box this shape would otherwise draw is suppressed client-side
    // (see GroundworkClient's BEFORE_BLOCK_OUTLINE registration) in favor of our own renderer.
}
