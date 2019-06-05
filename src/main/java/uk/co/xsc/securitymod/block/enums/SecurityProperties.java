package uk.co.xsc.securitymod.block.enums;

import net.minecraft.state.property.EnumProperty;

public class SecurityProperties {

    public static final EnumProperty<ElectronicTier> ELECTRONIC_TIER;

    static {
        ELECTRONIC_TIER = EnumProperty.create("electronic-tier", ElectronicTier.class);
    }

}
