package cnnr.groundwork.network;

import cnnr.groundwork.Groundwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C: tells the client which wisp (if any) the local player currently has selected for focus. */
public record WispSelectionPayload(boolean hasSelection, BlockPos pos) implements CustomPacketPayload {
    public static final Identifier ID = Groundwork.id("wisp_selection");
    public static final CustomPacketPayload.Type<WispSelectionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, WispSelectionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, WispSelectionPayload::hasSelection,
        BlockPos.STREAM_CODEC, WispSelectionPayload::pos,
        WispSelectionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
