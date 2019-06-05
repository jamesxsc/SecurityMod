package uk.co.xsc.securitymod.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import uk.co.xsc.securitymod.block.CameraBlock;
import uk.co.xsc.securitymod.block.enums.ElectronicTier;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityBlocks {

    public static final CameraBlock CAMERA_MECHANICAL_BLOCK = new CameraBlock(ElectronicTier.MECHANICAL);
    public static final CameraBlock CAMERA_ANALOG_BLOCK = new CameraBlock(ElectronicTier.ANALOG);
    public static final CameraBlock CAMERA_TRANSISTOR_BLOCK = new CameraBlock(ElectronicTier.TRANSISTOR);
    public static final CameraBlock CAMERA_SILICON_BLOCK = new CameraBlock(ElectronicTier.SILICON);
    public static final CameraBlock CAMERA_QUANTUM_BLOCK = new CameraBlock(ElectronicTier.QUANTUM);

    public static void init() {
        register("camera_mechanical_block", CAMERA_MECHANICAL_BLOCK);
        register("camera_analog_block", CAMERA_ANALOG_BLOCK);
        register("camera_transistor_block", CAMERA_TRANSISTOR_BLOCK);
        register("camera_silicon_block", CAMERA_SILICON_BLOCK);
        register("camera_quantum_block", CAMERA_QUANTUM_BLOCK);
    }

    private static void register(String id, Block block) {
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, id), block);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, id), block.asItem());
    }

}
