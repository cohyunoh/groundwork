package cnnr.groundwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Foundation-only for now: proves placement + persistence. Plan ownership arrives in a later stone. */
public class WispBlockEntity extends BlockEntity {
    private long createdTick = 0L;

    public WispBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WISP, pos, state);
    }

    public void setCreatedTick(long t) { this.createdTick = t; setChanged(); }
    public long getCreatedTick() { return createdTick; }

    @Override protected void saveAdditional(ValueOutput out) {
        out.putLong("created_tick", createdTick);
    }

    @Override protected void loadAdditional(ValueInput in) {
        this.createdTick = in.getLongOr("created_tick", 0L);
    }
}
