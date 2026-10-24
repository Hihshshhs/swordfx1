package com.swordfx;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /swordfx style <name>      sword animation
 * /swordfx ringstyle <name>  jump ring particle
 * /swordfx tilt <degrees>    static sword tilt
 * /swordfx scale <percent>   sword size (100 = normal)
 * /swordfx toggle <module>   turn a module on/off
 * /swordfx status            show current settings
 */
public final class SwordFxCommands {

	private static final List<String> MODULES = List.of(
			"infobar", "target", "stats", "armor",
			"swordanim", "replaceswing",
			"jumpring", "landring", "airswirl");

	private SwordFxCommands() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommands.literal("swordfx")
						.then(ClientCommands.literal("style")
								.then(ClientCommands.argument("name", StringArgumentType.word())
										.suggests((c, b) -> SharedSuggestionProvider.suggest(
												Arrays.stream(SwordFxConfig.SwingStyle.values()).map(SwordFxCommands::lower).toList(), b))
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											try {
												SwordFxConfig cfg = SwordFxConfig.get();
												cfg.swingStyle = SwordFxConfig.SwingStyle.valueOf(name.toUpperCase(Locale.ROOT));
												cfg.save();
												ctx.getSource().sendFeedback(Component.literal("Sword animation: " + lower(cfg.swingStyle)));
												return 1;
											} catch (IllegalArgumentException e) {
												ctx.getSource().sendFeedback(Component.literal("Unknown style. Try: "
														+ Arrays.stream(SwordFxConfig.SwingStyle.values()).map(SwordFxCommands::lower).toList()));
												return 0;
											}
										})))
						.then(ClientCommands.literal("ringstyle")
								.then(ClientCommands.argument("name", StringArgumentType.word())
										.suggests((c, b) -> SharedSuggestionProvider.suggest(
												Arrays.stream(SwordFxConfig.RingStyle.values()).map(SwordFxCommands::lower).toList(), b))
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											try {
												SwordFxConfig cfg = SwordFxConfig.get();
												cfg.ringStyle = SwordFxConfig.RingStyle.valueOf(name.toUpperCase(Locale.ROOT));
												cfg.save();
												ctx.getSource().sendFeedback(Component.literal("Ring style: " + lower(cfg.ringStyle)));
												return 1;
											} catch (IllegalArgumentException e) {
												ctx.getSource().sendFeedback(Component.literal("Unknown ring style. Try: "
														+ Arrays.stream(SwordFxConfig.RingStyle.values()).map(SwordFxCommands::lower).toList()));
												return 0;
											}
										})))
						.then(ClientCommands.literal("tilt")
								.then(ClientCommands.argument("degrees", IntegerArgumentType.integer(-60, 60))
										.executes(ctx -> {
											SwordFxConfig cfg = SwordFxConfig.get();
											cfg.tiltDegrees = IntegerArgumentType.getInteger(ctx, "degrees");
											cfg.save();
											ctx.getSource().sendFeedback(Component.literal("Sword tilt: " + cfg.tiltDegrees + " deg"));
											return 1;
										})))
						.then(ClientCommands.literal("scale")
								.then(ClientCommands.argument("percent", IntegerArgumentType.integer(50, 150))
										.executes(ctx -> {
											SwordFxConfig cfg = SwordFxConfig.get();
											cfg.swordScale = IntegerArgumentType.getInteger(ctx, "percent") / 100.0f;
											cfg.save();
											ctx.getSource().sendFeedback(Component.literal("Sword scale: " + Math.round(cfg.swordScale * 100) + "%"));
											return 1;
										})))
						.then(ClientCommands.literal("toggle")
								.then(ClientCommands.argument("module", StringArgumentType.word())
										.suggests((c, b) -> SharedSuggestionProvider.suggest(MODULES, b))
										.executes(ctx -> {
											String module = StringArgumentType.getString(ctx, "module").toLowerCase(Locale.ROOT);
											SwordFxConfig cfg = SwordFxConfig.get();
											Boolean value = toggle(cfg, module);
											if (value == null) {
												ctx.getSource().sendFeedback(Component.literal("Unknown module. Try: " + MODULES));
												return 0;
											}
											cfg.save();
											ctx.getSource().sendFeedback(Component.literal(module + ": " + (value ? "on" : "off")));
											return 1;
										})))
						.then(ClientCommands.literal("status")
								.executes(ctx -> {
									SwordFxConfig c = SwordFxConfig.get();
									ctx.getSource().sendFeedback(Component.literal(
											"style=" + lower(c.swingStyle) + " tilt=" + c.tiltDegrees
													+ " scale=" + Math.round(c.swordScale * 100) + "% ring=" + lower(c.ringStyle)
													+ " | infobar=" + c.infoBar + " target=" + c.targetHud
													+ " stats=" + c.statsPanel + " armor=" + c.armorPanel
													+ " | swordanim=" + c.swordAnimations + " replaceswing=" + c.replaceVanillaSwing
													+ " | jumpring=" + c.jumpRing + " landring=" + c.landRing + " airswirl=" + c.airSwirl));
									return 1;
								}))
		));
	}

	private static Boolean toggle(SwordFxConfig c, String module) {
		return switch (module) {
			case "infobar" -> c.infoBar = !c.infoBar;
			case "target" -> c.targetHud = !c.targetHud;
			case "stats" -> c.statsPanel = !c.statsPanel;
			case "armor" -> c.armorPanel = !c.armorPanel;
			case "swordanim" -> c.swordAnimations = !c.swordAnimations;
			case "replaceswing" -> c.replaceVanillaSwing = !c.replaceVanillaSwing;
			case "jumpring" -> c.jumpRing = !c.jumpRing;
			case "landring" -> c.landRing = !c.landRing;
			case "airswirl" -> c.airSwirl = !c.airSwirl;
			default -> null;
		};
	}

	private static String lower(Enum<?> e) {
		return e.name().toLowerCase(Locale.ROOT);
	}
}
