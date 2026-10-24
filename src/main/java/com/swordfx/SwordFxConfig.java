package com.swordfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config at .minecraft/config/swordfx.json. Change values in-game with /swordfx. */
public final class SwordFxConfig {

	public enum SwingStyle {
		VANILLA, SLASH, CHOP, STAB, SPIN, FLIP, SWEEP;

		public SwingStyle next() {
			SwingStyle[] all = values();
			return all[(ordinal() + 1) % all.length];
		}
	}

	public enum RingStyle {
		STARLIGHT, SOUL, FLAME, SPARK, CLOUD, GLOW
	}

	// HUD
	public boolean infoBar = true;        // name | ping | fps
	public boolean targetHud = true;      // enemy / animal health
	public boolean statsPanel = true;     // CPS, coords, direction
	public boolean armorPanel = true;     // armor durability

	// Sword
	public boolean swordAnimations = true;
	public boolean replaceVanillaSwing = true;
	public SwingStyle swingStyle = SwingStyle.SLASH;
	public int tiltDegrees = 18;          // static tilt while holding a sword
	public float swordScale = 1.0f;

	// Jump visuals
	public boolean jumpRing = true;
	public boolean landRing = true;
	public boolean airSwirl = false;
	public RingStyle ringStyle = RingStyle.STARLIGHT;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static SwordFxConfig instance;

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("swordfx.json");
	}

	public static SwordFxConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static SwordFxConfig load() {
		Path p = path();
		if (Files.exists(p)) {
			try {
				SwordFxConfig c = GSON.fromJson(Files.readString(p), SwordFxConfig.class);
				if (c != null) {
					if (c.swingStyle == null) c.swingStyle = SwingStyle.SLASH;
					if (c.ringStyle == null) c.ringStyle = RingStyle.STARLIGHT;
					return c;
				}
			} catch (Exception e) {
				SwordFxClient.LOGGER.warn("Could not read swordfx.json, using defaults", e);
			}
		}
		SwordFxConfig c = new SwordFxConfig();
		c.save();
		return c;
	}

	public void save() {
		try {
			Files.writeString(path(), GSON.toJson(this));
		} catch (IOException e) {
			SwordFxClient.LOGGER.warn("Could not save swordfx.json", e);
		}
	}
}
