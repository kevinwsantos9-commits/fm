package com.nivek.flash.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/** Raios desenhados como segmentos de linha (nao sao particulas). */
public final class LightningRenderer {
    private record Ctx(VertexConsumer vc, MatrixStack.Entry e, Vec3d cam) {}

    private LightningRenderer() {}

    public static void render(WorldRenderContext wc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        VertexConsumerProvider vcp = wc.consumers();
        if (p == null || mc.world == null || !ClientState.enabled || vcp == null) return;

        float td = mc.getRenderTickCounter().getTickDelta(false);
        Ctx c = new Ctx(vcp.getBuffer(RenderLayer.getLines()), wc.matrixStack().peek(), wc.camera().getPos());
        int level = ClientState.level;
        int rgb = ClientState.color;
        Vec3d pos = p.getLerpedPos(td);
        Vec3d vel = p.getVelocity();
        long time = mc.world.getTime();
        boolean moving = vel.horizontalLength() > 0.08;
        boolean first = mc.options.getPerspective().isFirstPerson();

        // Raios envolvendo o corpo (3a pessoa).
        if (!first) {
            int n = 2 + level / 3;
            if (!moving && !ClientState.vibrating) n = Math.max(2, n / 2);
            if (ClientState.vibrating) n += 2;
            for (int i = 0; i < n; i++) {
                arc(c, new Random((time / 2) * 7919L + i * 104729L), pos, rgb, 255);
            }
        }

        if (level >= 10) {
            // Faiscas ao redor do corpo.
            Random r = new Random(time * 31L + 7);
            int sparks = (level - 9) * 2;
            for (int i = 0; i < sparks; i++) {
                double ang = r.nextDouble() * Math.PI * 2, rad = 0.3 + r.nextDouble() * 0.7;
                bolt(c, r, pos.x + Math.cos(ang) * rad, pos.y + r.nextDouble() * 1.9, pos.z + Math.sin(ang) * rad,
                        r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5,
                        0.2 + r.nextDouble() * 0.35, 2, rgb, 220);
            }

            // Rastros que somem rapido.
            List<ClientState.Pt> trail = ClientState.TRAIL;
            int life = level - 2;
            int per = 1 + (level - 10) / 5;
            for (int i = 0; i < trail.size(); i++) {
                ClientState.Pt t = trail.get(i);
                double f = 1.0 - ((time + td) - t.tick()) / life;
                if (f <= 0) continue;
                int a = (int) (255 * f);
                Random tr = new Random(t.tick() * 131L);
                for (int j = 0; j < per; j++) {
                    bolt(c, tr, t.x(), t.y() + (tr.nextDouble() - 0.5) * 0.8, t.z(),
                            tr.nextDouble() - 0.5, tr.nextDouble() - 0.5, tr.nextDouble() - 0.5,
                            0.4 + tr.nextDouble() * 0.6, 3, rgb, a);
                }
                if (i > 0) {
                    ClientState.Pt q = trail.get(i - 1);
                    double dx = t.x() - q.x(), dy = t.y() - q.y(), dz = t.z() - q.z();
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > 0.01 && d < 6) bolt(c, tr, q.x(), q.y(), q.z(), dx, dy, dz, d, 3, rgb, a);
                }
            }

            // Nivel 20: raios longos para tras.
            if (level == 20 && moving) {
                Random r2 = new Random(time * 17L + 3);
                for (int k = 0; k < 3; k++) {
                    bolt(c, r2, pos.x, pos.y + 0.9, pos.z,
                            -vel.x + (r2.nextDouble() - 0.5) * 0.6, -vel.y * 0.3 + (r2.nextDouble() - 0.5) * 0.4,
                            -vel.z + (r2.nextDouble() - 0.5) * 0.6, 2.2 + r2.nextDouble() * 1.2, 8, rgb, 230);
                }
            }
        }

        if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
    }

    /** Raio em arco em volta do corpo, com pequenas ramificacoes. */
    private static void arc(Ctx c, Random r, Vec3d o, int rgb, int a) {
        double ang = r.nextDouble() * Math.PI * 2;
        double span = (r.nextBoolean() ? 1 : -1) * (1.0 + r.nextDouble());
        double y = o.y + 0.1 + r.nextDouble() * 1.5;
        double dy = (r.nextDouble() - 0.5) * 0.9;
        int segs = 6;
        double px = o.x + Math.cos(ang) * 0.5, py = y, pz = o.z + Math.sin(ang) * 0.5;
        for (int i = 1; i <= segs; i++) {
            double t = i / (double) segs;
            double an = ang + span * t, rad = 0.4 + r.nextDouble() * 0.3;
            double nx = o.x + Math.cos(an) * rad;
            double ny = y + dy * t + (r.nextDouble() - 0.5) * 0.25;
            double nz = o.z + Math.sin(an) * rad;
            seg(c, px, py, pz, nx, ny, nz, rgb, a);
            if (r.nextDouble() < 0.3) {
                bolt(c, r, nx, ny, nz, Math.cos(an) + (r.nextDouble() - 0.5), (r.nextDouble() - 0.5) * 1.2,
                        Math.sin(an) + (r.nextDouble() - 0.5), 0.35 + r.nextDouble() * 0.3, 3, rgb, a);
            }
            px = nx; py = ny; pz = nz;
        }
    }

    /** Raio reto "quebrado" numa direcao. */
    private static void bolt(Ctx c, Random r, double x, double y, double z, double dx, double dy, double dz,
                             double len, int segs, int rgb, int a) {
        double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6) return;
        dx /= l; dy /= l; dz /= l;
        double step = len / segs;
        for (int i = 0; i < segs; i++) {
            double nx = x + dx * step + (r.nextDouble() - 0.5) * step * 0.9;
            double ny = y + dy * step + (r.nextDouble() - 0.5) * step * 0.9;
            double nz = z + dz * step + (r.nextDouble() - 0.5) * step * 0.9;
            seg(c, x, y, z, nx, ny, nz, rgb, a);
            x = nx; y = ny; z = nz;
        }
    }

    /** Segmento com brilho (copia colorida deslocada) e nucleo claro. */
    private static void seg(Ctx c, double x1, double y1, double z1, double x2, double y2, double z2, int rgb, int a) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        line(c, x1 + 0.02, y1, z1 - 0.02, x2 + 0.02, y2, z2 - 0.02, r, g, b, a / 3);
        line(c, x1, y1, z1, x2, y2, z2, r + (255 - r) * 3 / 4, g + (255 - g) * 3 / 4, b + (255 - b) * 3 / 4, a);
    }

    private static void line(Ctx c, double x1, double y1, double z1, double x2, double y2, double z2,
                             int r, int g, int b, int a) {
        float ax = (float) (x1 - c.cam.x), ay = (float) (y1 - c.cam.y), az = (float) (z1 - c.cam.z);
        float bx = (float) (x2 - c.cam.x), by = (float) (y2 - c.cam.y), bz = (float) (z2 - c.cam.z);
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6f) return;
        dx /= l; dy /= l; dz /= l;
        c.vc.vertex(c.e, ax, ay, az).color(r, g, b, a).normal(c.e, dx, dy, dz);
        c.vc.vertex(c.e, bx, by, bz).color(r, g, b, a).normal(c.e, dx, dy, dz);
    }
}
