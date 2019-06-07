package uk.co.xsc.securitymod.block;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.ChatFormat;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import uk.co.xsc.securitymod.block.blockentity.CameraBlockEntity;
import uk.co.xsc.securitymod.block.enums.ElectronicTier;
import uk.co.xsc.securitymod.block.enums.SecurityProperties;
import uk.co.xsc.securitymod.util.*;

import java.util.Objects;
import java.util.function.Predicate;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class CameraBlock extends FacingBlock implements BlockEntityProvider, INetworkDevice {

    public static final DirectionProperty FACING;
    public static final EnumProperty<ElectronicTier> ELECTRONIC_TIER;

    static {
        FACING = FacingBlock.FACING;
        ELECTRONIC_TIER = SecurityProperties.ELECTRONIC_TIER;
    }

    private final ElectronicTier tier;

    public CameraBlock(ElectronicTier tier) {
        super(FabricBlockSettings.of(Material.METAL).breakInstantly().drops(new Identifier(MOD_ID, "camera_" + tier.asString() + "_block")).build());
        this.tier = tier;
        this.setDefaultState(this.getStateFactory().getDefaultState().with(FACING, Direction.NORTH).with(ELECTRONIC_TIER, this.tier));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext itemPlacementContext_1) {
        Direction direction = itemPlacementContext_1.getPlayerFacing();
        return this.getDefaultState().with(FACING, direction).with(ELECTRONIC_TIER, this.tier);
    }

    @Override
    protected void appendProperties(StateFactory.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(FACING, ELECTRONIC_TIER);
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockRenderType getRenderType(BlockState blockState_1) {
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean isSimpleFullBlock(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1) {
        return false;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new CameraBlockEntity();
    }

    @Override
    public Item asItem() {
        return TierUtil.asItem(this, tier);
    }

    @Override
    public boolean receives() {
        return false;
    }

    @Override
    public boolean transmits() {
        return false;
    }

    @Override
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    @Environment(EnvType.CLIENT)
    public Screen getConfigurationScreen(BlockEntity cameraBlockEntity) {
        Screen screen = new Screen(NarratorManager.EMPTY) {

            private final BlockEntity blockEntity;
            private int sliderWidth = 150;
            private int baseY;
            private int windowHeight;

            private FilteredTextFieldWidget addressBox1;
            private FilteredTextFieldWidget addressBox2;
            private FilteredTextFieldWidget addressBox3;
            private FilteredTextFieldWidget addressBox4;

            private Component addressValidity() {
                if (
                        addressBox1.getText().length() == 0 ||
                                addressBox2.getText().length() == 0 ||
                                addressBox3.getText().length() == 0 ||
                                addressBox4.getText().length() == 0
                ) {
                    return new TextComponent(" (Invalid)").setStyle(new Style().setColor(ChatFormat.DARK_RED));
                } else {
                    return new TextComponent(" (Valid)").setStyle(new Style().setColor(ChatFormat.DARK_GREEN));
                }
            }

            private FilteredTextFieldWidget scopeBox1;
            private FilteredTextFieldWidget scopeBox2;
            private FilteredTextFieldWidget scopeBox3;
            private FilteredTextFieldWidget scopeBox4;

            private Component scopeValidity() {
                if (
                        scopeBox1.getText().length() == 0 ||
                                scopeBox2.getText().length() == 0 ||
                                scopeBox3.getText().length() == 0 ||
                                scopeBox4.getText().length() == 0
                ) {
                    return new TextComponent(" (Invalid)").setStyle(new Style().setColor(ChatFormat.DARK_RED));
                } else {
                    return new TextComponent(" (Valid)").setStyle(new Style().setColor(ChatFormat.DARK_GREEN));
                }
            }

            {
                this.blockEntity = cameraBlockEntity;
                setSize(192, 192);
                baseY = (MinecraftClient.getInstance().window.getHeight() / 4) - (this.height / 2) - 12;
                windowHeight = MinecraftClient.getInstance().window.getHeight();
            }

            @SuppressWarnings("PointlessArithmeticExpression")
            @Override
            public void init() {
                assert (blockEntity instanceof CameraBlockEntity);
                super.init();

                CalculatedValueSliderWidget pan = new CalculatedValueSliderWidget(MinecraftClient.getInstance().options, this.width / 2 - sliderWidth / 2, baseY + 40, sliderWidth, 20, .5) {
                    {
                        this.value = (double) (((CameraBlockEntity) blockEntity).getPan() + 30) / 60;
                    }

                    @Override
                    public int getCalculatedValue() {
                        return (int) (value * 60 - 30);
                    }
                };

                CalculatedValueSliderWidget tilt = new CalculatedValueSliderWidget(MinecraftClient.getInstance().options, this.width / 2 - sliderWidth / 2, baseY + 75, sliderWidth, 20, .5) {
                    {
                        this.value = (double) (((CameraBlockEntity) blockEntity).getTilt() + 30) / 60;
                    }

                    @Override
                    public int getCalculatedValue() {
                        return (int) (value * 60 - 30);
                    }
                };

                this.addButton(pan);
                this.addButton(tilt);

                //network settings
                int addressBoxWidth = 27;
                int[] oldAddressValues = ArrayUtil.intFromStr(((CameraBlockEntity) blockEntity).getAddress().split("\\."), false, true);

                addressBox1 = new FilteredTextFieldWidget(this.font, this.width / 2 - (addressBoxWidth * 2 + 5), baseY + 132, addressBoxWidth, 15, "",
                        (Predicate<String>) s -> s.matches("^[0-9]*$"));
                addressBox1.setSuggestion("192");
                addressBox1.setMaxLength(3);
                addressBox2 = new FilteredTextFieldWidget(this.font, this.width / 2 - (addressBoxWidth * 1 + 0), baseY + 132, addressBoxWidth, 15, "",
                        (Predicate<String>) s -> s.matches("^[0-9]*$"));
                addressBox2.setSuggestion("168");
                addressBox2.setMaxLength(3);
                addressBox3 = new FilteredTextFieldWidget(this.font, this.width / 2 + (addressBoxWidth * 0 + 5), baseY + 132, addressBoxWidth, 15, "",
                        (Predicate<String>) s -> s.matches("^[0-9]*$"));
                addressBox3.setSuggestion("1");
                addressBox3.setMaxLength(3);
                addressBox4 = new FilteredTextFieldWidget(this.font, this.width / 2 + (addressBoxWidth * 1 + 10), baseY + 132, addressBoxWidth, 15, "",
                        (Predicate<String>) s -> s.matches("^[0-9]*$"));
                addressBox4.setSuggestion("135");
                addressBox4.setMaxLength(3);

                if (oldAddressValues.length == 4) {
                    addressBox1.setText(oldAddressValues[0] == 0x785 ? "" : String.valueOf(oldAddressValues[0]));
                    addressBox2.setText(oldAddressValues[1] == 0x785 ? "" : String.valueOf(oldAddressValues[1]));
                    addressBox3.setText(oldAddressValues[2] == 0x785 ? "" : String.valueOf(oldAddressValues[2]));
                    addressBox4.setText(oldAddressValues[3] == 0x785 ? "" : String.valueOf(oldAddressValues[3]));
                }

                this.addButton(addressBox1);
                this.addButton(addressBox2);
                this.addButton(addressBox3);
                this.addButton(addressBox4);

                int[] oldScopeValues = ArrayUtil.intFromStr(((CameraBlockEntity) blockEntity).getScope().split("\\."), true, true);

                scopeBox1 = new FilteredTextFieldWidget(this.font, this.width / 2 - (addressBoxWidth * 2 + 5), baseY + 164, addressBoxWidth, 15, "",
                        s -> s.matches("^[xX0-9]*$"));
                scopeBox1.setSuggestion("192");
                scopeBox1.setMaxLength(3);
                scopeBox2 = new FilteredTextFieldWidget(this.font, this.width / 2 - (addressBoxWidth * 1 + 0), baseY + 164, addressBoxWidth, 15, "",
                        s -> s.matches("^[xX0-9]*$"));
                scopeBox2.setSuggestion("168");
                scopeBox2.setMaxLength(3);
                scopeBox3 = new FilteredTextFieldWidget(this.font, this.width / 2 + (addressBoxWidth * 0 + 5), baseY + 164, addressBoxWidth, 15, "",
                        s -> s.matches("^[xX0-9]*$"));
                scopeBox3.setSuggestion("1");
                scopeBox3.setMaxLength(3);
                scopeBox4 = new FilteredTextFieldWidget(this.font, this.width / 2 + (addressBoxWidth * 1 + 10), baseY + 164, addressBoxWidth, 15, "",
                        s -> s.matches("^[xX0-9]*$"));
                scopeBox4.setSuggestion("x");

                if (oldScopeValues.length == 4) {
                    scopeBox1.setText(oldScopeValues[0] == 0x784 ? "X" : oldScopeValues[0] == 0x785 ? "" : String.valueOf(oldScopeValues[0]));
                    scopeBox2.setText(oldScopeValues[1] == 0x784 ? "X" : oldScopeValues[1] == 0x785 ? "" : String.valueOf(oldScopeValues[1]));
                    scopeBox3.setText(oldScopeValues[2] == 0x784 ? "X" : oldScopeValues[2] == 0x785 ? "" : String.valueOf(oldScopeValues[2]));
                    scopeBox4.setText(oldScopeValues[3] == 0x784 ? "X" : oldScopeValues[3] == 0x785 ? "" : String.valueOf(oldScopeValues[3]));
                }

                this.addButton(scopeBox1);
                this.addButton(scopeBox2);
                this.addButton(scopeBox3);
                this.addButton(scopeBox4);

                //control
                int buttonWidth = 40;
                ButtonWidget cancelButton = new ButtonWidget(this.width / 2 - (buttonWidth + 10), baseY + 183, buttonWidth, 20, "Cancel", new ButtonWidget.PressAction() {
                    @Override
                    public void onPress(ButtonWidget var1) {
                        MinecraftClient.getInstance().openScreen(null);
                    }
                });
                ButtonWidget saveButton = new ButtonWidget(this.width / 2 + 10, baseY + 183, buttonWidth, 20, "Save", new ButtonWidget.PressAction() {
                    @Override
                    public void onPress(ButtonWidget var1) {
                        CameraBlockEntity cbe = (CameraBlockEntity) blockEntity;
                        cbe.setPan(pan.getCalculatedValue());
                        cbe.setTilt(tilt.getCalculatedValue());
                        cbe.setAddress(addressBox1.getText() + "." + addressBox2.getText() + "." + addressBox3.getText() + "." + addressBox4.getText());
                        cbe.setScope(scopeBox1.getText() + "." + scopeBox2.getText() + "." + scopeBox3.getText() + "." + scopeBox4.getText());
                        MinecraftClient.getInstance().openScreen(null);
                    }
                });

                this.addButton(cancelButton);
                this.addButton(saveButton);
            }

            @Override
            public void tick() {
                super.tick();
            }

            @Override
            public void render(int int_1, int int_2, float float_1) {
                if (windowHeight != MinecraftClient.getInstance().window.getHeight()) {
                    MinecraftClient.getInstance().openScreen(null);
                    return;
                }
                Identifier BG_TEX = new Identifier(MOD_ID, "textures/gui/networked/icon.png");
                this.renderBackground();
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                Objects.requireNonNull(this.minecraft).getTextureManager().bindTexture(BG_TEX);
                int int_3 = (this.width - 192) / 2;
                int int_4 = (this.height - 192) / 2;
                this.blit(int_3, int_4, 0, 0, 192, 192);
                this.drawCenteredString(this.font, new TextComponent("Device Settings").setStyle(new Style().setUnderline(true)).getFormattedText(), this.width / 2, baseY + 15, 16777215);
                this.drawCenteredString(this.font, new TextComponent("Pan").getFormattedText(), this.width / 2, baseY + 30, 16777215);
                this.drawCenteredString(this.font, new TextComponent("Tilt").getFormattedText(), this.width / 2, baseY + 65, 16777215);

                //network settings
                this.drawCenteredString(this.font, new TextComponent("Network Settings").setStyle(new Style().setUnderline(true))
                        .getFormattedText(), this.width / 2, baseY + 105, 16777215);
                this.drawCenteredString(this.font, new TextComponent("Device Address")
                        .append(addressValidity())
                        .getFormattedText(), this.width / 2, baseY + 120, 16777215);
                this.drawCenteredString(this.font, new TextComponent("Scope")
                        .append(scopeValidity())
                        .getFormattedText(), this.width / 2, baseY + 152, 16777215);

                super.render(int_1, int_2, float_1);
            }

            @Override
            public void init(MinecraftClient minecraftClient_1, int int_1, int int_2) {

                super.init(minecraftClient_1, int_1, int_2);
            }

            @Override
            public Component getTitle() {
                return new TextComponent("Security Camera - Network Settings").setStyle(new Style().setBold(true));
            }
        };
        return screen;
    }

}
