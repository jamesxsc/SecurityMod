package uk.co.xsc.securitymod.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import uk.co.xsc.securitymod.block.CameraBlock;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityBlocks {

    public static final CameraBlock CAMERA_BLOCK = new CameraBlock();

    public static void init() {
        register("camera_block", CAMERA_BLOCK, new Item.Settings().itemGroup(ItemGroup.BREWING));
    }

    private static void register(String id, Block block, Item.Settings blockItemSettings) {
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, id), block);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, id), new BlockItem(block, blockItemSettings));
    }

}
