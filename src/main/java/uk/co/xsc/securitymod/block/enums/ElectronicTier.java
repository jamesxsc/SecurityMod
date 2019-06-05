package uk.co.xsc.securitymod.block.enums;

public enum ElectronicTier implements net.minecraft.util.StringIdentifiable {
    MECHANICAL("mechanical"),
    ANALOG("analog"),
    TRANSISTOR("transistor"),
    SILICON("silicon"),
    QUANTUM("quantum"),
    ;

    private final String string;

    ElectronicTier(String string) {
        this.string = string;
    }

    @Override
    public String asString() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

}
