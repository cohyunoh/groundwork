package cnnr.groundwork.network;

import cnnr.groundwork.Groundwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * One batch of a plan, server->client. `planId` groups batches of the same plan;
 * `first` marks the batch that should clear any prior client copy of this plan;
 * `last` marks the final batch so the client knows the plan is complete.
 */
public record PlanSyncPayload(UUID owner, int planId, boolean first, boolean last,
                               List<Entry> entries) implements CustomPacketPayload {

    public record Entry(BlockPos pos, byte changeType, BlockState original, BlockState built) {
        /** Which state best represents this ghost to a human: the original block for REMOVE
         *  (since built is air there), the placed/final block otherwise. */
        public BlockState displayState() {
            return changeType == 1 ? original : built; // 1 == ChangeType.REMOVE.ordinal()
        }
    }

    public static final Identifier ID = Groundwork.id("plan_sync");
    public static final CustomPacketPayload.Type<PlanSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, Entry::pos,
        ByteBufCodecs.BYTE, Entry::changeType,
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Entry::original,
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Entry::built,
        Entry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanSyncPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, PlanSyncPayload::owner,
        ByteBufCodecs.VAR_INT, PlanSyncPayload::planId,
        ByteBufCodecs.BOOL, PlanSyncPayload::first,
        ByteBufCodecs.BOOL, PlanSyncPayload::last,
        ENTRY_CODEC.apply(ByteBufCodecs.list()), PlanSyncPayload::entries,
        PlanSyncPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
