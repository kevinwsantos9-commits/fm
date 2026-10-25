package com.nivek.flash;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NivekFlash implements ModInitializer {
    public static final String MOD_ID = "nivekflash";
    private static final Identifier SPEED_ID = Identifier.of(MOD_ID, "speed");
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean enabled;
        int level = 1;
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(FlashPayload.ID, FlashPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(FlashPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            State s = STATES.computeIfAbsent(p.getUuid(), k -> new State());
            s.enabled = payload.enabled();
            s.level = Math.max(1, Math.min(20, payload.level()));
            FlashCommon.setVibrating(p.getUuid(), s.enabled && payload.vibrating());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                State s = STATES.get(p.getUuid());
                if (s != null) apply(p, s);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            STATES.remove(id);
            FlashCommon.VIBRATING.remove(id);
            FlashCommon.REL_SERVER.remove(id);
        });
    }

    private static void apply(ServerPlayerEntity p, State s) {
        EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr == null) return;
        EntityAttributeModifier cur = attr.getModifier(SPEED_ID);
        if (!s.enabled) {
            if (cur != null) attr.removeModifier(SPEED_ID);
            return;
        }
        double v = s.level * 0.25;
        if (cur == null || cur.value() != v) {
            attr.removeModifier(SPEED_ID);
            attr.addTemporaryModifier(new EntityAttributeModifier(SPEED_ID, v, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        // sem dano de queda e sem ficar sem fome para correr
        p.fallDistance = 0;
        if (p.getHungerManager().getFoodLevel() < 10) p.getHungerManager().setFoodLevel(10);
    }
}
