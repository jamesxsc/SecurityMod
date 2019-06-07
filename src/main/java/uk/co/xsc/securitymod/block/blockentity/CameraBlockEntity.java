package uk.co.xsc.securitymod.block.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.CompoundTag;
import uk.co.xsc.securitymod.registry.SecurityBlockEntities;
import uk.co.xsc.securitymod.util.INetworkDevice;

public class CameraBlockEntity extends BlockEntity {

    private int pan;
    private int tilt;
    private String scope;
    private String address;


    public CameraBlockEntity() {
        super(SecurityBlockEntities.CAMERA_BLOCK_ENTITY);
        pan = 0;
        tilt = 0;
        address = "unset";
        scope = "unset";
    }

    public CameraBlockEntity(BlockEntityType<?> blockEntityType_1) {
        super(blockEntityType_1);
    }

    public void takeSnapshot() {
        getWorld().rayTraceBlock(null, null, null, null, null);
    }

    @Override
    public void fromTag(CompoundTag compoundTag_1) {
        setPan(compoundTag_1.getInt("pan"));
        setTilt(compoundTag_1.getInt("tilt"));
        setAddress(compoundTag_1.getString("address"));
        setScope(compoundTag_1.getString("scope"));

        super.fromTag(compoundTag_1);
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag_1) {
        compoundTag_1.putInt("pan", getPan());
        compoundTag_1.putInt("tilt", getTilt());
        compoundTag_1.putString("address", getAddress());
        compoundTag_1.putString("scope", getScope());

        return super.toTag(compoundTag_1);
    }

    public int getPan() {
        return pan;
    }

    public int getTilt() {
        return tilt;
    }

    public String getScope() {
        return scope;
    }

    public String getAddress() {
        return address;
    }

    public void setPan(int pan) {
        this.pan = pan;
    }

    public void setTilt(int tilt) {
        this.tilt = tilt;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
