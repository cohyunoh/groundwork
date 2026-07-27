package cnnr.groundwork.network;

import cnnr.groundwork.Groundwork;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C2S: push (grow, delta=+1) or pull (shrink, delta=-1) one face of the region being edited. */
public record PushRegionFacePayload(Direction face, int delta) implements CustomPacketPayload {
    public static final Identifier ID = Groundwork.id("push_region_face");
    public static final CustomPacketPayload.Type<PushRegionFacePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PushRegionFacePayload> CODEC = StreamCodec.composite(
        Direction.STREAM_CODEC, PushRegionFacePayload::face,
        ByteBufCodecs.VAR_INT, PushRegionFacePayload::delta,
        PushRegionFacePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
