package com.swordfx;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;

/** Top info bar (name / ping / FPS), target health, CPS + coords panel and armor panel. */
public final class SwordFxHud {

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFAAAAAA;
	private static final int ACCENT = 0xFF35E0FF;
	private static final int PANEL_BG = 0x90000000;

	private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
	private static boolean wasAttackDown;
	private static boolean wasUseDown;

	private static LivingEntity target;
	private static long targetSeenAt;
	private static LivingEntity shownFor;
	private static float shownHealth;

	private SwordFxHud() {}

	public static void register() {
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("swordfx", "hud"),
				SwordFxHud::extract);
		ClientTickEvents.END_CLIENT_TICK.register(SwordFxHud::tick);
	}

	// ------------------------------------------------------------------ tick

	private static void tick(Minecraft mc) {
		long now = System.currentTimeMillis();

		boolean attack = mc.options.keyAttack.isDown();
		if (attack && !wasAttackDown) LEFT_CLICKS.addLast(now);
		wasAttackDown = attack;

		boolean use = mc.options.keyUse.isDown();
		if (use && !wasUseDown) RIGHT_CLICKS.addLast(now);
		wasUseDown = use;

		prune(LEFT_CLICKS, now);
		prune(RIGHT_CLICKS, now);

		Entity looked = mc.crosshairPickEntity;
		if (looked instanceof LivingEntity living && living.isAlive()) {
			target = living;
			targetSeenAt = now;
		}
		if (target != null && (target.isRemoved() || now - targetSeenAt > 3000)) {
			target = null;
		}
	}

	private static void prune(ArrayDeque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
			clicks.pollFirst();
		}
	}

	// ---------------------------------------------------------------- render

	private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.options.hideGui) return;

		SwordFxConfig cfg = SwordFxConfig.get();
		Font font = mc.font;
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();

		if (cfg.infoBar) drawInfoBar(g, font, mc, player, screenW);
		if (cfg.statsPanel) drawStats(g, font, player);
		if (cfg.armorPanel) drawArmor(g, font, player, screenW);
		if (cfg.targetHud && target != null) drawTarget(g, font, player, screenW, screenH);
	}

	private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, PANEL_BG);
		g.fill(x, y, x + w, y + 1, ACCENT);
	}

	// ------------------------------------------------------------- info bar

	private static void drawInfoBar(GuiGraphicsExtractor g, Font font, Minecraft mc, LocalPlayer player, int screenW) {
		String name = mc.getUser().getName();

		ClientPacketListener conn = mc.getConnection();
		PlayerInfo info = conn == null ? null : conn.getPlayerInfo(player.getUUID());
		int ping = info == null ? -1 : info.getLatency();
		String pingText = ping < 0 ? "-- ms" : ping + " ms";
		int fps = mc.getFps();
		String fpsText = fps + " FPS";

		String sep = "  |  ";
		int total = font.width(name) + font.width(sep) * 2 + font.width(pingText) + font.width(fpsText);
		int padX = 8;
		int w = total + padX * 2;
		int x = (screenW - w) / 2;
		int y = 4;

		panel(g, x, y, w, 16);

		int tx = x + padX;
		int ty = y + 4;
		g.text(font, name, tx, ty, WHITE, true);
		tx += font.width(name);
		g.text(font, sep, tx, ty, GREY, true);
		tx += font.width(sep);
		g.text(font, pingText, tx, ty, pingColor(ping), true);
		tx += font.width(pingText);
		g.text(font, sep, tx, ty, GREY, true);
		tx += font.width(sep);
		g.text(font, fpsText, tx, ty, fpsColor(fps), true);
	}

	private static int pingColor(int ping) {
		if (ping < 0) return GREY;
		if (ping < 60) return 0xFF55FF55;
		if (ping < 150) return 0xFFFFFF55;
		return 0xFFFF5555;
	}

	private static int fpsColor(int fps) {
		if (fps >= 100) return 0xFF55FF55;
		if (fps >= 40) return 0xFFFFFF55;
		return 0xFFFF5555;
	}

	// ---------------------------------------------------------- stats panel

	private static void drawStats(GuiGraphicsExtractor g, Font font, LocalPlayer player) {
		String[] lines = {
				"CPS  " + LEFT_CLICKS.size() + " | " + RIGHT_CLICKS.size(),
				String.format("XYZ  %.1f  %.1f  %.1f", player.getX(), player.getY(), player.getZ()),
				"Facing  " + compass(player.getYRot())
		};

		int w = 0;
		for (String line : lines) w = Math.max(w, font.width(line));
		w += 10;
		int h = lines.length * 11 + 6;

		int x = 4;
		int y = 4;
		panel(g, x, y, w, h);
		for (int i = 0; i < lines.length; i++) {
			g.text(font, lines[i], x + 5, y + 5 + i * 11, WHITE, true);
		}
	}

	private static String compass(float yaw) {
		String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		float wrapped = ((yaw % 360.0f) + 360.0f) % 360.0f;
		int index = Math.floorMod(Math.round(wrapped / 45.0f), 8);
		return names[index];
	}

	// ---------------------------------------------------------- armor panel

	private static void drawArmor(GuiGraphicsExtractor g, Font font, LocalPlayer player, int screenW) {
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		String[] labels = {"Helmet", "Chest", "Legs", "Boots"};

		String[] lines = new String[5];
		int[] colors = new int[5];
		int count = 0;

		for (int i = 0; i < slots.length; i++) {
			ItemStack stack = player.getItemBySlot(slots[i]);
			if (stack.isEmpty()) continue;
			lines[count] = durabilityLine(labels[i], stack);
			colors[count] = durabilityColor(stack);
			count++;
		}
		ItemStack held = player.getMainHandItem();
		if (!held.isEmpty() && held.isDamageableItem()) {
			lines[count] = durabilityLine("Hand", held);
			colors[count] = durabilityColor(held);
			count++;
		}
		if (count == 0) return;

		int w = 0;
		for (int i = 0; i < count; i++) w = Math.max(w, font.width(lines[i]));
		w += 10;
		int h = count * 11 + 6;
		int x = screenW - w - 4;
		int y = 4;

		panel(g, x, y, w, h);
		for (int i = 0; i < count; i++) {
			g.text(font, lines[i], x + 5, y + 5 + i * 11, colors[i], true);
		}
	}

	private static String durabilityLine(String label, ItemStack stack) {
		if (!stack.isDamageableItem()) return label + "  --";
		int left = stack.getMaxDamage() - stack.getDamageValue();
		int pct = Math.round(100.0f * left / stack.getMaxDamage());
		return label + "  " + left + " (" + pct + "%)";
	}

	private static int durabilityColor(ItemStack stack) {
		if (!stack.isDamageableItem()) return WHITE;
		float ratio = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
		return ratioColor(ratio);
	}

	// ----------------------------------------------------------- target HUD

	private static void drawTarget(GuiGraphicsExtractor g, Font font, LocalPlayer player, int screenW, int screenH) {
		LivingEntity e = target;
		if (e == null) return;

		float health = e.getHealth();
		float max = Math.max(1.0f, e.getMaxHealth());
		float absorb = e.getAbsorptionAmount();

		if (shownFor != e) {
			shownFor = e;
			shownHealth = health;
		}
		shownHealth += (health - shownHealth) * 0.12f;

		String name = e.getDisplayName().getString();
		int nameColor = e instanceof Player ? 0xFF55FFFF : (e instanceof Enemy ? 0xFFFF5555 : 0xFF55FF55);

		String hpText = String.format("%.1f / %.1f", health, max);
		if (absorb > 0.0f) hpText += String.format(" +%.1f", absorb);
		String extra = String.format("%.1fm", player.distanceTo(e)) + "  ARM " + e.getArmorValue();

		int w = 140;
		int h = 50;
		int x = (screenW - w) / 2;
		int y = screenH / 2 + 24;

		panel(g, x, y, w, h);

		g.text(font, name, x + (w - font.width(name)) / 2, y + 5, nameColor, true);

		int barX = x + 8;
		int barY = y + 17;
		int barW = w - 16;
		int barH = 6;
		g.fill(barX, barY, barX + barW, barY + barH, 0xFF202020);

		float ratio = Mth.clamp(health / max, 0.0f, 1.0f);
		float shownRatio = Mth.clamp(shownHealth / max, 0.0f, 1.0f);

		// trailing (lost health) segment, then the real bar on top
		g.fill(barX, barY, barX + Math.round(barW * shownRatio), barY + barH, 0xFFB0B0B0);
		g.fill(barX, barY, barX + Math.round(barW * ratio), barY + barH, ratioColor(ratio));

		if (absorb > 0.0f) {
			float aRatio = Mth.clamp(absorb / max, 0.0f, 1.0f);
			g.fill(barX, barY + barH, barX + Math.round(barW * aRatio), barY + barH + 1, 0xFFFFD83A);
		}

		g.text(font, hpText, x + (w - font.width(hpText)) / 2, y + 26, WHITE, true);
		g.text(font, extra, x + (w - font.width(extra)) / 2, y + 38, GREY, true);
	}

	/** red (0) -> yellow (0.5) -> green (1) */
	private static int ratioColor(float ratio) {
		ratio = Mth.clamp(ratio, 0.0f, 1.0f);
		int r = Math.min(255, Math.round((1.0f - ratio) * 2.0f * 255.0f));
		int gr = Math.min(255, Math.round(ratio * 2.0f * 255.0f));
		return 0xFF000000 | (r << 16) | (gr << 8) | 0x30;
	}
}
