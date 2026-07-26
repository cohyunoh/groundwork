package cnnr.groundwork.network;

import cnnr.groundwork.Groundwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C: tells the client whether the local player's build-vision (ghost rendering) is on or off. */
public record BuildVisionPayload(boolean active) implements CustomPacketPayload {
    public static final Identifier ID = Groundwork.id("build_vision");
    public static final CustomPacketPayload.Type<BuildVisionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BuildVisionPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, BuildVisionPayload::active, BuildVisionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
