package com.swordfx.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.swordfx.SwordAnimator;
import com.swordfx.SwordFxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the first-person arm+item render call with our own pose.
 *
 * The method is listed under both its 26.x name (submitArmWithItem) and its old
 * name (renderArmWithItem). Parameters are grabbed by type/ordinal with
 * MixinExtras @Local instead of spelling out the whole descriptor, so small
 * signature changes between versions will not break it. The config has
 * "required": false, so if the target ever can't be found the game still runs
 * (just without sword animations).
 *
 * Float args of that method, in order: partialTicks (0), pitch (1),
 * swingProgress (2), equippedProgress (3).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class HandRendererMixin {

	@Inject(method = {"submitArmWithItem", "renderArmWithItem"}, at = @At("HEAD"), require = 0)
	private void swordfx$begin(CallbackInfo ci,
	                           @Local(argsOnly = true) PoseStack pose,
	                           @Local(argsOnly = true) InteractionHand hand,
	                           @Local(argsOnly = true) ItemStack stack,
	                           @Local(argsOnly = true, ordinal = 2) LocalFloatRef swing) {
		SwordAnimator.pushed = false;

		SwordFxConfig cfg = SwordFxConfig.get();
		if (!cfg.swordAnimations || !stack.is(ItemTags.SWORDS)) {
			return;
		}

		boolean rightHanded = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT;
		float mainSide = rightHanded ? 1.0f : -1.0f;
		float side = hand == InteractionHand.MAIN_HAND ? mainSide : -mainSide;

		float progress = swing.get();

		pose.pushPose();
		SwordAnimator.pushed = true;
		SwordAnimator.apply(pose, side, progress, cfg);

		// Our swing replaces the vanilla one, otherwise the two would stack.
		if (cfg.replaceVanillaSwing && cfg.swingStyle != SwordFxConfig.SwingStyle.VANILLA) {
			swing.set(0.0f);
		}
	}

	@Inject(method = {"submitArmWithItem", "renderArmWithItem"}, at = @At("RETURN"), require = 0)
	private void swordfx$end(CallbackInfo ci, @Local(argsOnly = true) PoseStack pose) {
		if (SwordAnimator.pushed) {
			pose.popPose();
			SwordAnimator.pushed = false;
		}
	}
}
