package uk.co.xsc.securitymod;

import net.fabricmc.api.ModInitializer;
import uk.co.xsc.securitymod.registry.SecurityBlockEntities;
import uk.co.xsc.securitymod.registry.SecurityBlocks;
import uk.co.xsc.securitymod.registry.SecurityItems;

public class SecurityMod implements ModInitializer {

	@Override
	public void onInitialize() {
		SecurityItems.init();
		SecurityBlocks.init();
		SecurityBlockEntities.init();

		new Thread() {
			@Override
			public void run() {
				Thread.currentThread().setName("Set security world testing thread daemon non applicable");
				//SecurityWorldCameraRenderTestRenderer securityWorldCameraRenderTestRenderer = new SecurityWorldCameraRenderTestRenderer();
				super.run();
			}
		}.start();
	}

}
