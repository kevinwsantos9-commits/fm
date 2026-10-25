package com.nivek.flash;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FlashPayload(int level, boolean enabled, boolean vibrating) implements CustomPayload {
    public static final CustomPayload.Id<FlashPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NivekFlash.MOD_ID, "state"));

    public static final PacketCodec<RegistryByteBuf, FlashPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, FlashPayload::level,
            PacketCodecs.BOOL, FlashPayload::enabled,
            PacketCodecs.BOOL, FlashPayload::vibrating,
            FlashPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
