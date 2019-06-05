package uk.co.xsc.securitymod.block;

import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
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

import static uk.co.xsc.securitymod.Constants.MOD_ID;

public class CameraBlock extends FacingBlock implements BlockEntityProvider {

    public static final DirectionProperty FACING;
    public static final EnumProperty<ElectronicTier> ELECTRONIC_TIER;

    static {
        FACING = FacingBlock.FACING;
        ELECTRONIC_TIER = SecurityProperties.ELECTRONIC_TIER;
    }

    public CameraBlock() {
        super(FabricBlockSettings.of(Material.METAL).breakInstantly().drops(new Identifier(MOD_ID, "camera_block")).build());
        this.setDefaultState(this.getStateFactory().getDefaultState().with(FACING, Direction.NORTH).with(ELECTRONIC_TIER, ElectronicTier.MECHANICAL));
    }

//    @Override
//    protected void appendProperties(StateFactory.Builder<Block, BlockState> stateBuilder) {
//        stateBuilder.add(FACING, ELECTRONIC_TIER);
//    }

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
}
