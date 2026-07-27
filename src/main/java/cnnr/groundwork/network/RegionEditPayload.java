package cnnr.groundwork.network;

import cnnr.groundwork.Groundwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C: tells the client whether it's editing a wisp's region, and which wisp (by position). */
public record RegionEditPayload(boolean editing, BlockPos wispPos) implements CustomPacketPayload {
    public static final Identifier ID = Groundwork.id("region_edit");
    public static final CustomPacketPayload.Type<RegionEditPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RegionEditPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, RegionEditPayload::editing,
        BlockPos.STREAM_CODEC, RegionEditPayload::wispPos,
        RegionEditPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
