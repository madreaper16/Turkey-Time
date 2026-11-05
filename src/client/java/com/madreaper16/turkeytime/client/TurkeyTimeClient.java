package com.madreaper16.turkeytime.client;

import com.madreaper16.turkeytime.TurkeyTime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class TurkeyTimeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(TurkeyModel.TOM, () -> TurkeyModel.createLayer(true, false));
        ModelLayerRegistry.registerModelLayer(TurkeyModel.HEN, () -> TurkeyModel.createLayer(false, false));
        ModelLayerRegistry.registerModelLayer(TurkeyModel.POULT, () -> TurkeyModel.createLayer(false, true));
        EntityRenderers.register(TurkeyTime.TURKEY, TurkeyRenderer::new);
        EntityRenderers.register(TurkeyTime.TURKEY_EGG_PROJECTILE, ThrownItemRenderer::new);
    }
}
