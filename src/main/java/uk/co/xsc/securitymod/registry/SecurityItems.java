package uk.co.xsc.securitymod.registry;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import uk.co.xsc.securitymod.item.Cat9GConfigToolItem;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityItems {

    public static final Cat9GConfigToolItem CAT_9G_CONFIG_TOOL_ITEM = new Cat9GConfigToolItem();

    public static void init() {
        register("cat_9g_config_tool_item", CAT_9G_CONFIG_TOOL_ITEM);
    }

    private static void register(String id, Item item) {
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, id), item);
    }

}
