package bat.batcg;

import bat.batcg.belt.ModBeltComponents;
import bat.batcg.block.ModBlocks;
import bat.batcg.card.ModBoosterComponents;
import bat.batcg.card.ModCardComponents;
import bat.batcg.item.ModItems;
import bat.batcg.network.BatcgPackets;
import bat.batcg.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Batcg implements ModInitializer {

	public static final String MOD_ID = "batcg";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.RegisterModItems();
		ModBlocks.RegisterModBlocks();
		ModCardComponents.initialize();
		ModBoosterComponents.initialize();
		ModBeltComponents.initialize();

		ModScreenHandlers.init();

		// Networking (incluye OpenBeltC2SPayload) -> SOLO aquí
		BatcgPackets.registerPayloads();
		BatcgPackets.initServerReceivers();

		LOGGER.info("BATCG LOADED!");
	}
}
