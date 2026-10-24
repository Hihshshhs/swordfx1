package com.swordfx;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwordFxClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("swordfx");

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("swordfx", "main"));

	private static KeyMapping cycleKey;

	@Override
	public void onInitializeClient() {
		SwordFxConfig.get();

		cycleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.swordfx.cycle",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_G,
				CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (cycleKey.consumeClick()) {
				if (client.player == null) return;
				SwordFxConfig cfg = SwordFxConfig.get();
				cfg.swingStyle = cfg.swingStyle.next();
				cfg.save();
				client.player.sendSystemMessage(Component.literal("Sword animation: "
						+ cfg.swingStyle.name().toLowerCase()));
			}
		});

		SwordFxHud.register();
		JumpEffects.register();
		SwordFxCommands.register();

		LOGGER.info("SwordFX loaded");
	}
}
