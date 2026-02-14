package bat.batcg;

import bat.batcg.card.ModBoosterComponents;
import bat.batcg.card.ModCardComponents;
import bat.batcg.command.ModCommands;
import bat.batcg.belt.BeltHooks;
import bat.batcg.belt.ModBeltComponents;
import bat.batcg.item.ModItems;
import bat.batcg.network.BatcgPackets;
import bat.batcg.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import bat.batcg.block.ModBlocks;
import bat.batcg.screen.ModScreenHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Batcg implements ModInitializer {
	public static final String MOD_ID = "batcg";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.RegisterModItems();
		ModCardComponents.initialize();
		ModCommands.register();
		ModBoosterComponents.initialize();
		BatcgPackets.registerPayloads();
		BatcgPackets.initServerReceivers();
		BeltHooks.init();
		ModBlocks.RegisterModBlocks();
		ModBeltComponents.initialize();
		ModScreenHandlers.init();





		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("BATCG LOADED!");
	}
}