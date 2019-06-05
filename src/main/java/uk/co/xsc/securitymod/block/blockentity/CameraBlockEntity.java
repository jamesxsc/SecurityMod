package uk.co.xsc.securitymod.block.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.EnumProperty;
import uk.co.xsc.securitymod.registry.SecurityBlockEntities;
import uk.co.xsc.securitymod.util.INetworkDevice;

public class CameraBlockEntity extends BlockEntity implements INetworkDevice {

    public CameraBlockEntity() {
        super(SecurityBlockEntities.CAMERA_BLOCK_ENTITY);
    }

    @Override
    public boolean receives() {
        // for control data
        return true;
    }

    @Override
    public boolean transmits() {
        // for video data
        return true;
    }

}
