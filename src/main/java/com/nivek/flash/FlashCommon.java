package com.nivek.flash;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlashCommon {
    public static final Set<UUID> VIBRATING = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> REL_CLIENT = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> REL_SERVER = ConcurrentHashMap.newKeySet();

    private FlashCommon() {}

    public static void setVibrating(UUID id, boolean on) {
        if (on) VIBRATING.add(id); else VIBRATING.remove(id);
    }

    /** Chamado antes do tick de movimento do jogador (cliente e servidor). */
    public static void tickHook(PlayerEntity p) {
        UUID id = p.getUuid();
        boolean client = p.getWorld().isClient;
        Set<UUID> rel = client ? REL_CLIENT : REL_SERVER;
        if (VIBRATING.contains(id)) {
            p.noClip = true;
            rel.add(id);
            if (client) p.setNoGravity(true);
        } else if (rel.contains(id)) {
            // Ao desligar, mantem noClip ate o jogador estar em espaco livre (evita ficar preso).
            if (p.getWorld().isSpaceEmpty(p, p.getBoundingBox())) {
                rel.remove(id);
                if (client) p.setNoGravity(false);
            } else {
                p.noClip = true;
                if (client) p.setNoGravity(true);
            }
        }
    }
}
