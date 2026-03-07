package bat.batcg;

import bat.batcg.belt.BeltHooks; // ✅ ADD
import bat.batcg.belt.ModBeltComponents;
import bat.batcg.block.ModBlocks;
import bat.batcg.block.entity.ModBlockEntities;
import bat.batcg.card.ModBoosterComponents;
import bat.batcg.card.ModCardComponents;
import bat.batcg.command.ModCommands;
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
		ModBlockEntities.init();

		ModScreenHandlers.init();
		ModCommands.register();

		BatcgPackets.registerPayloads();
		BatcgPackets.initServerReceivers();

		BeltHooks.init(); // ✅ ADD (esto hace que se apliquen los atributos del cinturón)

		LOGGER.info("BATCG LOADED!");
	}
}