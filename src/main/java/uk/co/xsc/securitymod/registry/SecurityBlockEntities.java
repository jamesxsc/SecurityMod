package uk.co.xsc.securitymod.registry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityBlockEntities {

    public static void init() {

    }

    private static void register(String id, BlockEntityType<? extends BlockEntity> blockEntityType) {
        Registry.register(Registry.BLOCK_ENTITY, new Identifier(MOD_ID, id), blockEntityType);
    }

}
