package uk.co.xsc.securitymod.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;

public interface INetworkDevice {

    boolean receives();

    boolean transmits();

    Screen getConfigurationScreen(BlockEntity blockEntity);

    default void onReceive() {}

}
