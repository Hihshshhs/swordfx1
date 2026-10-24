package com.swordfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/**
 * Applies the custom sword pose. Everything is rotated around the point where the
 * right hand normally sits in first person, so the sword pivots from the hand
 * instead of swinging around the camera.
 *
 * If a rotation looks mirrored or backwards in game, flip the sign of that angle below.
 */
public final class SwordAnimator {

	/** True when the mixin pushed a pose that must be popped again at the end of the method. */
	public static boolean pushed;

	// Approximate first-person hand position (view space).
	private static final float PX = 0.56f, PY = -0.52f, PZ = -0.72f;

	private SwordAnimator() {}

	/**
	 * @param side  +1 for right hand, -1 for left hand
	 * @param p     vanilla swing progress, 0..1 (0 while idle)
	 */
	public static void apply(PoseStack pose, float side, float p, SwordFxConfig cfg) {
		pose.translate(side * PX, PY, PZ);

		if (cfg.swordScale != 1.0f) {
			pose.scale(cfg.swordScale, cfg.swordScale, cfg.swordScale);
		}

		// Static tilt while holding the sword.
		pose.mulPose(Axis.ZP.rotationDegrees(-side * cfg.tiltDegrees));

		if (p > 0.0f && cfg.swingStyle != SwordFxConfig.SwingStyle.VANILLA) {
			swing(pose, side, p, cfg.swingStyle);
		}

		pose.translate(-side * PX, -PY, -PZ);
	}

	private static void swing(PoseStack pose, float side, float p, SwordFxConfig.SwingStyle style) {
		float a = Mth.sin(p * Mth.PI);                 // 0 -> 1 -> 0
		float b = Mth.sin(Mth.sqrt(p) * Mth.PI);       // fast start, slow end
		float s = smooth(p);                            // 0 -> 1 eased

		switch (style) {
			case SLASH -> {
				// wide diagonal slash across the screen
				pose.mulPose(Axis.ZP.rotationDegrees(-side * 80.0f * b));
				pose.mulPose(Axis.XP.rotationDegrees(-35.0f * a));
				pose.mulPose(Axis.YP.rotationDegrees(side * 35.0f * a));
			}
			case CHOP -> {
				// wind up, smash down, recover
				float x;
				if (p < 0.30f) {
					x = -75.0f * smooth(p / 0.30f);
				} else if (p < 0.60f) {
					x = Mth.lerp(smooth((p - 0.30f) / 0.30f), -75.0f, 55.0f);
				} else {
					x = Mth.lerp(smooth((p - 0.60f) / 0.40f), 55.0f, 0.0f);
				}
				pose.mulPose(Axis.XP.rotationDegrees(x));
				pose.translate(0.0f, 0.0f, -0.15f * a);
			}
			case STAB -> {
				// point forward and thrust
				pose.mulPose(Axis.XP.rotationDegrees(-65.0f * a));
				pose.translate(0.0f, 0.0f, -0.85f * a);
			}
			case SPIN -> {
				// full roll around the view axis
				pose.mulPose(Axis.ZP.rotationDegrees(-side * 360.0f * s));
				pose.translate(0.0f, 0.0f, -0.25f * a);
			}
			case FLIP -> {
				// forward flip of the blade
				pose.mulPose(Axis.XP.rotationDegrees(-360.0f * s));
				pose.translate(0.0f, 0.12f * a, -0.2f * a);
			}
			case SWEEP -> {
				// low sideways sweep, pushes the blade out
				pose.mulPose(Axis.YP.rotationDegrees(side * 55.0f * b));
				pose.mulPose(Axis.XP.rotationDegrees(-20.0f * a));
				pose.translate(-side * 0.25f * a, 0.0f, -0.3f * a);
			}
			default -> {
			}
		}
	}

	/** Smoothstep easing. */
	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0f, 1.0f);
		return t * t * (3.0f - 2.0f * t);
	}
}
