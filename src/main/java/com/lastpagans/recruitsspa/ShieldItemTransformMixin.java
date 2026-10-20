package com.lastpagans.recruitsspa;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes previewed shields use the same base display transform as the paper reference item. */
@Mixin(value = ItemInHandRenderer.class, priority = 900)
abstract class ShieldItemTransformMixin {
    @Unique
    private static final ItemStack PAPER_REFERENCE = new ItemStack(Items.PAPER);

    @Inject(method = "renderItem", at = @At("HEAD"), remap = false, require = 0)
    private void recruitsSpa$correctShieldTransformDev(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callback
    ) {
        recruitsSpa$correctShieldTransform(entity, stack, displayContext, leftHand, poseStack);
    }

    // Forge's production name for ItemInHandRenderer#renderItem in Minecraft 1.20.1.
    @Inject(method = "m_269530_", at = @At("HEAD"), remap = false, require = 0)
    private void recruitsSpa$correctShieldTransformProduction(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callback
    ) {
        recruitsSpa$correctShieldTransform(entity, stack, displayContext, leftHand, poseStack);
    }

    @Unique
    private static void recruitsSpa$correctShieldTransform(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack
    ) {
        if (!(entity instanceof AbstractRecruitEntity recruit)
                || !(stack.getItem() instanceof ShieldItem)
                || ClientAnimationTestState.get(recruit.getId()) == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel shieldModel = minecraft.getItemRenderer().getModel(
                stack,
                entity.level(),
                entity,
                entity.getId()
        );
        BakedModel paperModel = minecraft.getItemRenderer().getModel(
                PAPER_REFERENCE,
                entity.level(),
                entity,
                entity.getId()
        );

        Matrix4f shieldTransform = recruitsSpa$modelTransform(shieldModel, displayContext, leftHand);
        Matrix4f paperTransform = recruitsSpa$modelTransform(paperModel, displayContext, leftHand);

        // Player Animator has already applied the authored leftItem transform.
        // Convert the model-specific shield transform into the paper transform
        // expected by the Blockbench reference: authored * correction * shield
        // becomes authored * paper.
        Matrix4f correction = paperTransform.mul(shieldTransform.invert());
        poseStack.mulPoseMatrix(correction);
    }

    @Unique
    private static Matrix4f recruitsSpa$modelTransform(
            BakedModel model,
            ItemDisplayContext displayContext,
            boolean leftHand
    ) {
        PoseStack transformStack = new PoseStack();
        ItemTransform transform = model.getTransforms().getTransform(displayContext);
        transform.apply(leftHand, transformStack);
        return new Matrix4f(transformStack.last().pose());
    }
}
