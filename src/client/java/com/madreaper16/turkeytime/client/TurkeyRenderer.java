package com.madreaper16.turkeytime.client;

import com.madreaper16.turkeytime.TurkeyTime;
import com.madreaper16.turkeytime.entity.Turkey;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;

public final class TurkeyRenderer extends MobRenderer<Turkey, TurkeyRenderState, TurkeyModel> {
    private final TurkeyModel tom, hen, poult;
    private static final Identifier[] TOM_TEXTURES = textures("tom");
    private static final Identifier[] HEN_TEXTURES = textures("hen");
    private static final Identifier POULT_TEXTURE = TurkeyTime.id("textures/entity/turkey/poult.png");

    private static Identifier[] textures(String sex) {
        return new Identifier[] {TurkeyTime.id("textures/entity/turkey/" + sex + "_0.png"),
                TurkeyTime.id("textures/entity/turkey/" + sex + "_1.png"),
                TurkeyTime.id("textures/entity/turkey/" + sex + "_2.png")};
    }

    public TurkeyRenderer(EntityRendererProvider.Context context) {
        super(context, new TurkeyModel(context.bakeLayer(TurkeyModel.TOM)), 0.35F);
        tom = model;
        hen = new TurkeyModel(context.bakeLayer(TurkeyModel.HEN));
        poult = new TurkeyModel(context.bakeLayer(TurkeyModel.POULT));
    }

    @Override
    public TurkeyRenderState createRenderState() {
        return new TurkeyRenderState();
    }

    @Override
    public void extractRenderState(Turkey turkey, TurkeyRenderState state, float partialTick) {
        super.extractRenderState(turkey, state, partialTick);
        state.male = turkey.isMale();
        state.autumn = turkey.isAutumn();
        state.plumage = turkey.getPlumage();
        state.display = turkey.getDisplay();
        state.displayAge = Math.max(0, turkey.level().getGameTime() - turkey.getDisplayStart() + partialTick);
        // Stationary NoAI display mobs can have onGround=false immediately after summon.
        state.airborne = !turkey.onGround() && !turkey.isInWater()
                && turkey.getDeltaMovement().lengthSqr() > 0.001;
        state.panicking = turkey.isPanicking();
    }

    @Override
    public void submit(TurkeyRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.isBaby ? poult : state.male ? tom : hen;
        super.submit(state, poses, collector, camera);
    }

    @Override
    protected void scale(TurkeyRenderState state, PoseStack poses) {
        poses.scale(state.ageScale, state.ageScale, state.ageScale);
    }

    @Override
    protected float getShadowRadius(TurkeyRenderState state) { return 0.35F * state.ageScale; }

    @Override
    protected AABB getBoundingBoxForCulling(Turkey turkey, float partialTick) {
        return super.getBoundingBoxForCulling(turkey, partialTick).inflate(0.6 * turkey.getAgeScale());
    }

    @Override
    public Identifier getTextureLocation(TurkeyRenderState state) {
        if (state.autumn) return TurkeyTime.id("textures/entity/turkey/" +
                (state.isBaby ? "poult" : state.male ? "tom" : "hen") + "_autumn.png");
        return state.isBaby ? POULT_TEXTURE : (state.male ? TOM_TEXTURES : HEN_TEXTURES)[Math.clamp(state.plumage, 0, 2)];
    }
}
