package uk.co.xsc.securitymod.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityBlocks {

    public static void init() {

    }

    private static void register(String id, Block block, Item.Settings blockItemSettings) {
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, id), block);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, id), new BlockItem(block, blockItemSettings));
    }

}
