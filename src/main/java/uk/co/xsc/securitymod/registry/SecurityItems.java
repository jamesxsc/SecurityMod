package uk.co.xsc.securitymod.registry;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class SecurityItems {

    public static void init() {

    }

    private static void register(String id, Item item) {
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, id), item);
    }

}
