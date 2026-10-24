package com.swordfx;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Expanding particle rings at your feet when you jump / land, plus an optional
 * air swirl. Particles are client-side only (nobody else sees them), and need no
 * world-render hooks, so they are very stable across versions.
 */
public final class JumpEffects {

	private static boolean wasOnGround = true;
	private static int airTicks = 0;

	private JumpEffects() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(JumpEffects::tick);
	}

	private static ParticleOptions particle(SwordFxConfig.RingStyle style) {
		return switch (style) {
			case STARLIGHT -> ParticleTypes.END_ROD;
			case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
			case FLAME -> ParticleTypes.FLAME;
			case SPARK -> ParticleTypes.ELECTRIC_SPARK;
			case CLOUD -> ParticleTypes.CLOUD;
			case GLOW -> ParticleTypes.GLOW;
		};
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null) {
			wasOnGround = true;
			airTicks = 0;
			return;
		}

		SwordFxConfig cfg = SwordFxConfig.get();
		boolean onGround = player.onGround();

		if (wasOnGround && !onGround && player.getDeltaMovement().y > 0.1 && cfg.jumpRing) {
			burst(level, player.getX(), player.getY(), player.getZ(), cfg.ringStyle, 1.0f);
		}

		if (!wasOnGround && onGround && airTicks > 6 && cfg.landRing) {
			burst(level, player.getX(), player.getY(), player.getZ(), cfg.ringStyle, 0.7f);
		}

		if (!onGround) {
			airTicks++;
			if (cfg.airSwirl && !player.isSpectator()) {
				swirl(level, player.getX(), player.getY(), player.getZ(), cfg.ringStyle, airTicks);
			}
		} else {
			airTicks = 0;
		}

		wasOnGround = onGround;
	}

	/** Three rings with different speeds so they spread out as a layered circle. */
	private static void burst(ClientLevel level, double x, double y, double z,
	                          SwordFxConfig.RingStyle style, float strength) {
		ParticleOptions type = particle(style);
		double[] speeds = {0.07, 0.13, 0.19};
		for (double speed : speeds) {
			int count = 28;
			for (int i = 0; i < count; i++) {
				double angle = (Math.PI * 2.0 * i) / count;
				double cos = Math.cos(angle);
				double sin = Math.sin(angle);
				double v = speed * strength;
				level.addParticle(type, x + cos * 0.35, y + 0.06, z + sin * 0.35, cos * v, 0.0, sin * v);
			}
		}
	}

	/** Two particles orbiting your feet while airborne. */
	private static void swirl(ClientLevel level, double x, double y, double z,
	                          SwordFxConfig.RingStyle style, int t) {
		ParticleOptions type = particle(style);
		for (int k = 0; k < 2; k++) {
			double angle = t * 0.55 + k * Math.PI;
			level.addParticle(type, x + Math.cos(angle) * 0.6, y + 0.1, z + Math.sin(angle) * 0.6, 0.0, 0.0, 0.0);
		}
	}
}
