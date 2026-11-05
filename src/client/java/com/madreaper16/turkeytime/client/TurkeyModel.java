package com.madreaper16.turkeytime.client;

import com.madreaper16.turkeytime.TurkeyTime;
import com.madreaper16.turkeytime.entity.Turkey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Original block geometry and articulated fan; shared rig, three baked silhouettes. */
public final class TurkeyModel extends EntityModel<TurkeyRenderState> {
    public static final ModelLayerLocation TOM = layer("tom");
    public static final ModelLayerLocation HEN = layer("hen");
    public static final ModelLayerLocation POULT = layer("poult");
    private final ModelPart body, neck, head, leftWing, rightWing, leftLeg, rightLeg, tail;
    private final ModelPart[] feathers = new ModelPart[7];

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(TurkeyTime.id("turkey"), name);
    }

    public TurkeyModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = root.getChild("neck");
        head = neck.getChild("head");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        tail = body.getChild("tail");
        for (int i = 0; i < feathers.length; i++) feathers[i] = tail.getChild("feather_" + i);
    }

    public static LayerDefinition createLayer(boolean male, boolean baby) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float width = baby ? 8 : male ? 9 : 8;
        float length = baby ? 9 : male ? 11 : 10;
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-width / 2, -4, -length / 2, width, 8, length), PartPose.offset(0, 17, 0));
        body.addOrReplaceChild("breast", CubeListBuilder.create().texOffs(0, 64)
                .addBox(-3, -2, -1, 6, 5, 2), PartPose.offset(0, -1, -length / 2));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0, -1, -4, 2, baby ? 5 : 7, baby ? 7 : 9), PartPose.offset(width / 2 - 0.5F, -2, 0));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(-2, -1, -4, 2, baby ? 5 : 7, baby ? 7 : 9), PartPose.offset(-width / 2 + 0.5F, -2, 0));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.5F, -1.5F, -1.5F, 3, baby ? 3 : 5, 3), PartPose.offset(0, baby ? 14 : 14.5F, -4));
        float headWidth = baby ? 5 : 4;
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-headWidth / 2, -2, -3, headWidth, baby ? 5 : 4, 5), PartPose.offset(0, baby ? -1 : -2.5F, -1.5F));
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(88, 0)
                .addBox(-1.5F, 0, -5, 3, 1.5F, 2), PartPose.ZERO);
        head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(116, 0)
                .addBox(headWidth / 2, -0.75F, -2, 0.15F, 1, 1), PartPose.ZERO);
        head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(116, 0)
                .addBox(-headWidth / 2 - 0.15F, -0.75F, -2, 0.15F, 1, 1), PartPose.ZERO);
        head.addOrReplaceChild("wattle", CubeListBuilder.create().texOffs(104, 0)
                .addBox(-0.5F, 1, -3.2F, 1, baby ? 0 : male ? 4 : 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("snood", CubeListBuilder.create().texOffs(104, 0)
                .addBox(-0.5F, -1.5F, -3.8F, 1, baby ? 0 : male ? 3 : 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", leg(), PartPose.offset(2, 20, 0));
        root.addOrReplaceChild("right_leg", leg(), PartPose.offset(-2, 20, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0, -1, length / 2 - 1, -0.65F, 0, 0));
        for (int i = 0; i < 7; i++) {
            float featherLength = baby ? 4 : male ? 10 : 7;
            tail.addOrReplaceChild("feather_" + i, CubeListBuilder.create().texOffs(32, 32)
                    .addBox(-1.5F, -featherLength, -0.5F, 3, featherLength, 1),
                    PartPose.offset(0, 0, (i % 2) * 0.15F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static CubeListBuilder leg() {
        return CubeListBuilder.create().texOffs(88, 16).addBox(-0.5F, 0, -0.5F, 1, 3, 1)
                .texOffs(96, 16).addBox(-1.5F, 3, -2.5F, 3, 1, 4);
    }

    @Override
    public void setupAnim(TurkeyRenderState s) {
        super.setupAnim(s);
        float time = s.ageInTicks;
        float walk = s.walkAnimationSpeed;
        float step = (float) Math.cos(s.walkAnimationPos * 0.8F) * 1.15F * walk;
        leftLeg.xRot = step;
        rightLeg.xRot = -step;
        body.zRot = (float) Math.sin(s.walkAnimationPos * 0.8F) * walk * 0.04F;
        body.y += (float) Math.sin(time * 0.08F) * 0.08F;
        neck.xRot = s.xRot * ((float) Math.PI / 180) * 0.5F;
        neck.yRot = s.yRot * ((float) Math.PI / 180);
        head.xRot = neck.xRot;
        head.y += (float) Math.sin(time * 0.09F) * 0.08F;

        float envelope = Math.clamp(Math.min(s.displayAge / 8, (Turkey.PRESENTATION_DURATION - s.displayAge) / 8), 0, 1);
        float strut = s.display == Turkey.STRUT && s.male && !s.isBaby ? envelope : 0;
        float forage = s.display == Turkey.FORAGE ? envelope : 0;
        float peck = (0.5F + 0.5F * (float) Math.sin(s.displayAge * 0.5F)) * forage;
        neck.xRot += peck * 1.15F;
        neck.y += peck * 5.5F;
        neck.z -= peck * 1.5F;
        head.xRot += peck * 0.3F;
        body.xRot = -strut * 0.12F;
        neck.z += strut * 0.7F;
        leftWing.zRot = strut * 0.3F;
        rightWing.zRot = -strut * 0.3F;
        tail.xRot += strut * 0.65F;
        for (int i = 0; i < feathers.length; i++) {
            float spread = s.isBaby ? 0.10F : s.male ? 0.13F + strut * 0.25F : 0.11F;
            feathers[i].zRot = (i - 3) * spread;
        }
        if (s.airborne || s.display == Turkey.FLUTTER) {
            float flap = 0.8F + (float) Math.sin(time * 1.5F) * 0.6F;
            leftWing.zRot = -flap;
            rightWing.zRot = flap;
            leftLeg.xRot = rightLeg.xRot = 0.25F;
        }
        if (s.panicking || s.display == Turkey.STARTLED) {
            neck.xRot -= 0.25F;
            tail.xRot -= 0.15F;
        }
    }
}
