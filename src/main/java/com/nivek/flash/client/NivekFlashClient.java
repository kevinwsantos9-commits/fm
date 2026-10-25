package com.nivek.flash.client;

import com.nivek.flash.FlashCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class NivekFlashClient implements ClientModInitializer {
    private static KeyBinding toggle, up, down, vib, cfg;

    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.nivekflash." + name, InputUtil.Type.KEYSYM, code, "key.categories.nivekflash"));
    }

    @Override
    public void onInitializeClient() {
        ClientState.load();
        toggle = key("toggle", GLFW.GLFW_KEY_G);
        up = key("up", GLFW.GLFW_KEY_UP);
        down = key("down", GLFW.GLFW_KEY_DOWN);
        vib = key("vib", GLFW.GLFW_KEY_B);
        cfg = key("cfg", GLFW.GLFW_KEY_K);

        HudRenderCallback.EVENT.register(FlashHud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(LightningRenderer::render);
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> ClientState.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ClientState.reset());
        ClientTickEvents.END_CLIENT_TICK.register(NivekFlashClient::tick);
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;

        while (toggle.wasPressed()) {
            ClientState.enabled = !ClientState.enabled;
            if (!ClientState.enabled) ClientState.vibrating = false;
            ClientState.sync();
        }
        while (up.wasPressed()) {
            ClientState.level = Math.min(20, ClientState.level + 1);
            ClientState.sync();
        }
        while (down.wasPressed()) {
            ClientState.level = Math.max(1, ClientState.level - 1);
            ClientState.sync();
        }
        while (vib.wasPressed()) {
            if (ClientState.enabled) {
                ClientState.vibrating = !ClientState.vibrating;
                ClientState.sync();
            }
        }
        while (cfg.wasPressed()) mc.setScreen(new ColorScreen());

        // Ao desligar a vibracao dentro de blocos, empurra para cima ate sair.
        Vec3d v = p.getVelocity();
        if (!ClientState.vibrating && FlashCommon.REL_CLIENT.contains(p.getUuid())
                && !w.isSpaceEmpty(p, p.getBoundingBox())) {
            p.setVelocity(v.x, 0.2, v.z);
        }

        long now = w.getTime();
        if (!ClientState.enabled) {
            ClientState.TRAIL.clear();
            return;
        }
        int level = ClientState.level;

        // Vibracao: voo vertical controlado (sem gravidade), nunca abaixo do fundo do mundo.
        if (ClientState.vibrating) {
            double vy = 0;
            if (mc.options.jumpKey.isPressed()) vy = 0.3;
            else if (mc.options.sneakKey.isPressed()) vy = -0.3;
            if (vy < 0 && p.getY() <= w.getBottomY() + 1) vy = 0;
            p.setVelocity(v.x, vy, v.z);
        }

        // No ar a velocidade horizontal do jogo e fixa; compensa proporcionalmente ao nivel.
        if (!p.isOnGround() && !p.isTouchingWater() && !p.isFallFlying() && !p.getAbilities().flying) {
            float f = p.input.movementForward, s = p.input.movementSideways;
            double len = Math.sqrt(f * f + s * s);
            if (len > 0.01) {
                if (len > 1) { f /= len; s /= len; }
                double yaw = Math.toRadians(p.getYaw());
                double sin = Math.sin(yaw), cos = Math.cos(yaw);
                double a = 0.0075 * level * (p.isSprinting() ? 1.0 : 0.77);
                p.addVelocity((s * cos - f * sin) * a, 0, (f * cos + s * sin) * a);
            }
        }

        // Correr na agua (nivel 5+), sem alterar blocos.
        if (!ClientState.vibrating && level >= 5 && !p.isSneaking() && !mc.options.jumpKey.isPressed()
                && p.getVelocity().horizontalLengthSquared() > 0.01) {
            waterRun(p, w);
        }

        // Rastros (nivel 10+).
        if (level >= 10 && p.getVelocity().horizontalLength() > 0.15) {
            ClientState.TRAIL.add(new ClientState.Pt(p.getX(), p.getY() + 0.9, p.getZ(), now));
        }
        int life = level - 2;
        ClientState.TRAIL.removeIf(t -> now - t.tick() > life);
    }

    private static void waterRun(ClientPlayerEntity p, ClientWorld w) {
        BlockPos feet = p.getBlockPos();
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos pos = feet.add(0, dy, 0);
            FluidState fs = w.getFluidState(pos);
            if (!fs.isIn(FluidTags.WATER) || !w.getFluidState(pos.up()).isEmpty()) continue;
            double top = pos.getY() + fs.getHeight(w, pos);
            if (p.getY() <= top + 0.1 && p.getY() >= top - 0.9) {
                Vec3d v = p.getVelocity();
                p.setPosition(p.getX(), top + 0.01, p.getZ());
                p.setVelocity(v.x, 0, v.z);
                p.setOnGround(true);
                p.fallDistance = 0;
                return;
            }
        }
    }
}
