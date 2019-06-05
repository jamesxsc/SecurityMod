package uk.co.xsc.securitymod.block;

import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
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
import uk.co.xsc.securitymod.util.TierUtil;

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class CameraBlock extends FacingBlock implements BlockEntityProvider {

    public static final DirectionProperty FACING;
    public static final EnumProperty<ElectronicTier> ELECTRONIC_TIER;

    static {
        FACING = FacingBlock.FACING;
        ELECTRONIC_TIER = SecurityProperties.ELECTRONIC_TIER;
    }

    private final ElectronicTier tier;

    public CameraBlock(ElectronicTier tier) {
        super(FabricBlockSettings.of(Material.METAL).breakInstantly().drops(new Identifier(MOD_ID, "camera_" + tier.asString() + "_block")).build());
        System.out.println(tier.asString());
        System.out.println(this.getDropTableId().getPath());
        System.out.println(this.getDropTableId().getNamespace());
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

}
