package uk.co.xsc.securitymod.util;

import net.minecraft.ChatFormat;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.World;
import uk.co.xsc.securitymod.block.enums.ElectronicTier;

import java.util.List;

public class TierUtil {

    public static Item asItem(Block block, ElectronicTier tier) {
        BlockItem blockItem = new BlockItem(block, new Item.Settings().itemGroup(ItemGroup.BREWING)) {

            private final ElectronicTier electronicTier = tier;

            @Override
            public void buildTooltip(ItemStack itemStack_1, World world_1, List<Component> list_1, TooltipContext tooltipContext_1) {
                list_1.add(new TextComponent("Tier:").setStyle(new Style().setUnderline(true).setColor(ChatFormat.WHITE))
                        .append(new TextComponent(" " + electronicTier.getDisplayName()).setStyle(
                                new Style().setColor(electronicTier.getColor()).setUnderline(false))));
            }

        };
        blockItem.registerBlockItemMap(Item.BLOCK_ITEM_MAP, blockItem);
        return blockItem;
    }

}
