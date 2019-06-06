package uk.co.xsc.securitymod.item;

import net.minecraft.ChatFormat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import uk.co.xsc.securitymod.util.INetworkDevice;

import java.util.List;
import java.util.Objects;

public class Cat9GConfigToolItem extends Item {

    public Cat9GConfigToolItem() {
        super(new Item.Settings().itemGroup(ItemGroup.BREWING).stackSize(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext itemUsageContext_1) {
        if (itemUsageContext_1.getHand().equals(Hand.MAIN_HAND)) {
            BlockState blockState = itemUsageContext_1.getWorld().getBlockState(itemUsageContext_1.getBlockPos());
            if (blockState.getBlock() instanceof INetworkDevice) {
                if (itemUsageContext_1.isPlayerSneaking()) {
                    itemUsageContext_1.getWorld().setBlockState(itemUsageContext_1.getBlockPos(), Blocks.AIR.getDefaultState());
                    if (!itemUsageContext_1.getWorld().isClient && !Objects.requireNonNull(itemUsageContext_1.getPlayer()).isCreative()) {
                        ItemStack is = itemUsageContext_1.getItemStack();
                        Block.dropStacks(blockState, itemUsageContext_1.getWorld(),
                                itemUsageContext_1.getBlockPos(), null, itemUsageContext_1.getPlayer(), is);
                    }
                } else {

                }
            }
        }
        return super.useOnBlock(itemUsageContext_1);
    }

    @Override
    public void buildTooltip(ItemStack itemStack_1, World world_1, List<Component> list_1, TooltipContext tooltipContext_1) {
        if (Screen.hasShiftDown()) {
            list_1.add(new TextComponent("Sneak & use on security").setStyle(new Style().setItalic(true).setColor(ChatFormat.WHITE)));
            list_1.add(new TextComponent("devices to remove").setStyle(new Style().setItalic(true).setColor(ChatFormat.WHITE)));
            list_1.add(new TextComponent("Hold ").setStyle(new Style().setItalic(false).setColor(ChatFormat.DARK_GRAY).setStrikethrough(true)).append(
                    new TextComponent("SHIFT").setStyle(new Style().setColor(ChatFormat.YELLOW).setItalic(true)
                            .setUnderline(true).setStrikethrough(true))
            ));
        } else {
            list_1.add(new TextComponent("Use on security devices").setStyle(new Style().setItalic(true).setColor(ChatFormat.WHITE)));
            list_1.add(new TextComponent("to configure").setStyle(new Style().setItalic(true).setColor(ChatFormat.WHITE)));
            list_1.add(new TextComponent("Hold ").setStyle(new Style().setColor(ChatFormat.DARK_GRAY)).append(
                    new TextComponent("SHIFT").setStyle(new Style().setColor(ChatFormat.YELLOW).setItalic(true)
                            .setUnderline(true))
            ));
        }
        super.buildTooltip(itemStack_1, world_1, list_1, tooltipContext_1);
    }

}
