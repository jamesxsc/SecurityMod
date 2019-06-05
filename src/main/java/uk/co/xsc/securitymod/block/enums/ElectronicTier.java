package uk.co.xsc.securitymod.block.enums;

import net.minecraft.ChatFormat;

public enum ElectronicTier implements net.minecraft.util.StringIdentifiable {
    MECHANICAL("mechanical", ChatFormat.GRAY, "Mechanical"),
    ANALOG("analog", ChatFormat.GOLD, "Analog"),
    TRANSISTOR("transistor", ChatFormat.DARK_GRAY, "Transistor"),
    SILICON("silicon", ChatFormat.LIGHT_PURPLE, "Silicon"),
    QUANTUM("quantum", ChatFormat.GREEN, "Quantum"),
    ;

    private final String string;
    private final ChatFormat color;
    private final String displayName;

    ElectronicTier(String string, ChatFormat color, String displayName) {
        this.string = string;
        this.color = color;
        this.displayName = displayName;
    }

    public static ElectronicTier getFromDamage(int damage) {
        switch (damage) {
            case 0:
                return MECHANICAL;
            case 1:
                return ANALOG;
            case 2:
                return TRANSISTOR;
            case 3:
                return SILICON;
            case 4:
                return QUANTUM;
            default:
                return MECHANICAL;
        }
    }

    @Override
    public String asString() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    public ChatFormat getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

}
