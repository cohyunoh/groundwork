package cnnr.groundwork.block;

import cnnr.groundwork.selection.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persists a placement timestamp and the wisp's region (the volume its future plan will occupy). */
public class WispBlockEntity extends BlockEntity {
    private long createdTick = 0L;
    private Selection region = new Selection();

    public WispBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WISP, pos, state);
    }

    public void setCreatedTick(long t) { this.createdTick = t; setChanged(); }
    public long getCreatedTick() { return createdTick; }

    public Selection getRegion() { return region; }
    public boolean hasRegion() { return region.isComplete(); }

    /** Replaces the region and pushes the change to tracking clients (region is render-relevant). */
    public void setRegion(Selection s) {
        this.region = s;
        setChanged();
        if (getLevel() instanceof ServerLevel sl) {
            sl.getChunkSource().blockChanged(getBlockPos());
        }
    }

    @Override protected void saveAdditional(ValueOutput out) {
        out.putLong("created_tick", createdTick);
        if (region.isComplete()) {
            BlockPos a = region.getCornerA();
            BlockPos b = region.getCornerB();
            out.putInt("region_ax", a.getX());
            out.putInt("region_ay", a.getY());
            out.putInt("region_az", a.getZ());
            out.putInt("region_bx", b.getX());
            out.putInt("region_by", b.getY());
            out.putInt("region_bz", b.getZ());
            out.putString("region_dim", region.getDimension().identifier().toString());
        }
    }

    @Override protected void loadAdditional(ValueInput in) {
        this.createdTick = in.getLongOr("created_tick", 0L);

        Selection s = new Selection();
        in.getString("region_dim").ifPresent(dimStr -> {
            ResourceKey<Level> dim = ResourceKey.create(Level.OVERWORLD.registryKey(), Identifier.parse(dimStr));
            BlockPos a = new BlockPos(in.getIntOr("region_ax", 0), in.getIntOr("region_ay", 0), in.getIntOr("region_az", 0));
            BlockPos b = new BlockPos(in.getIntOr("region_bx", 0), in.getIntOr("region_by", 0), in.getIntOr("region_bz", 0));
            s.setCornerA(a, dim);
            s.setCornerB(b, dim);
        });
        this.region = s;
    }

    // Client sync: the default getUpdatePacket() sends nothing and the default getUpdateTag() is
    // empty, so without these overrides the client's copy of this block-entity would never learn
    // about the region and the renderer couldn't draw it. Both delegate straight into the same
    // saveAdditional/loadAdditional used for disk persistence, so there's no duplicate logic.
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
