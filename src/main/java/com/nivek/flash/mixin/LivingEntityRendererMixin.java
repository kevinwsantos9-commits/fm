package com.nivek.flash.mixin;

import com.nivek.flash.FlashCommon;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void nivekflash$vibrate(LivingEntity e, float yaw, float tickDelta, MatrixStack ms,
                                    VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (e instanceof PlayerEntity && FlashCommon.VIBRATING.contains(e.getUuid())) {
            ms.translate((Math.random() - 0.5) * 0.07, (Math.random() - 0.5) * 0.02, (Math.random() - 0.5) * 0.07);
        }
    }
}
