package uk.co.xsc.securitymod.block.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.Screen;
import uk.co.xsc.securitymod.registry.SecurityBlockEntities;
import uk.co.xsc.securitymod.util.INetworkDevice;

public class CameraBlockEntity extends BlockEntity {

    public CameraBlockEntity() {
        super(SecurityBlockEntities.CAMERA_BLOCK_ENTITY);
        pan = 0;
        tilt = 0;
    }

    public CameraBlockEntity(BlockEntityType<?> blockEntityType_1) {
        super(blockEntityType_1);
    }

    public void takeSnapshot() {
        getWorld().rayTraceBlock(null, null, null, null, null);
    }

    private int pan;
    private int tilt;

    public int getPan() {
        return pan;
    }

    public int getTilt() {
        return tilt;
    }

}
