package com.nivek.flash.client;

import com.nivek.flash.FlashCommon;
import com.nivek.flash.FlashPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClientState {
    public record Pt(double x, double y, double z, long tick) {}

    public static boolean enabled = false;
    public static boolean vibrating = false;
    public static int level = 1;
    public static int color = 0xFFE000;
    public static final List<Pt> TRAIL = new ArrayList<>();

    private ClientState() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nivekflash.txt");
    }

    public static void load() {
        try {
            color = Integer.parseInt(Files.readString(file()).trim(), 16) & 0xFFFFFF;
        } catch (Exception ignored) {}
    }

    public static void setColor(int c) {
        color = c & 0xFFFFFF;
        try {
            Files.writeString(file(), Integer.toHexString(color));
        } catch (Exception ignored) {}
    }

    public static void reset() {
        enabled = false;
        vibrating = false;
        TRAIL.clear();
        FlashCommon.VIBRATING.clear();
        FlashCommon.REL_CLIENT.clear();
    }

    public static void sync() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        FlashCommon.setVibrating(mc.player.getUuid(), enabled && vibrating);
        if (ClientPlayNetworking.canSend(FlashPayload.ID)) {
            ClientPlayNetworking.send(new FlashPayload(level, enabled, vibrating));
        }
    }
}
