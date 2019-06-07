package uk.co.xsc.securitymod.util;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.options.GameOptions;

public abstract class CalculatedValueSliderWidget extends SliderWidget {

    protected CalculatedValueSliderWidget(GameOptions gameOptions_1, int int_1, int int_2, int int_3, int int_4, double double_1) {
        super(int_1, int_2, int_3, int_4, double_1);
    }

    @Override
    protected void updateMessage() {
    }

    @Override
    protected void applyValue() {
    }

    public abstract int getCalculatedValue();

}
