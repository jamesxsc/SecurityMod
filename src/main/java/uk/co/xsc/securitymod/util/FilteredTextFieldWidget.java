package uk.co.xsc.securitymod.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.function.Predicate;

public class FilteredTextFieldWidget extends TextFieldWidget {

    private final Predicate<String> accept;
    private String text = "";
    private String suggestion = "";

    @Override
    public void setSuggestion(String string_1) {
        this.suggestion = string_1;
        super.setSuggestion(string_1);
    }

    public FilteredTextFieldWidget(TextRenderer textRenderer_1, int int_1, int int_2, int int_3, int int_4, String string_1,
                                   Predicate<String> accept) {
        super(textRenderer_1, int_1, int_2, int_3, int_4, string_1);
        this.accept = accept;

        this.setChangedListener((str) -> {
            if (str.length() > 0) {
                super.setSuggestion("");
            } else {
                super.setSuggestion(suggestion);
            }
            if (!accept.test(str)) {
                setText(text);
            } else {
                this.text = str;
            }
        });
    }

}
