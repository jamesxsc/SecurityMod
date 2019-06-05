package uk.co.xsc.securitymod.registry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import uk.co.xsc.securitymod.block.blockentity.CameraBlockEntity;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityBlockEntities {

    public static final BlockEntityType<CameraBlockEntity> CAMERA_BLOCK_ENTITY = BlockEntityType.Builder.create(CameraBlockEntity::new).build(null);

    public static void init() {
        register("camera_block", CAMERA_BLOCK_ENTITY);
    }

    private static void register(String id, BlockEntityType<? extends BlockEntity> blockEntityType) {
        Registry.register(Registry.BLOCK_ENTITY, new Identifier(MOD_ID, id), blockEntityType);
    }

}
