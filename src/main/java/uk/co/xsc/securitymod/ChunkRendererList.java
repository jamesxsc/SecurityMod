package uk.co.xsc.securitymod;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public abstract class ChunkRendererList {
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    protected final List<ChunkRenderer> chunkRenderers = Lists.newArrayListWithCapacity(17424);
    protected boolean isCameraPositionSet;

    public void setCameraPosition(double double_1, double double_2, double double_3) {
        this.isCameraPositionSet = true;
        this.chunkRenderers.clear();
        this.cameraX = double_1;
        this.cameraY = double_2;
        this.cameraZ = double_3;
    }

    public void translateToOrigin(ChunkRenderer chunkRenderer_1) {
        BlockPos blockPos_1 = chunkRenderer_1.getOrigin();
        GlStateManager.translatef((float)((double)blockPos_1.getX() - this.cameraX), (float)((double)blockPos_1.getY() - this.cameraY), (float)((double)blockPos_1.getZ() - this.cameraZ));
    }

    public void add(ChunkRenderer chunkRenderer_1, BlockRenderLayer blockRenderLayer_1) {
        this.chunkRenderers.add(chunkRenderer_1);
    }

    public abstract void render(BlockRenderLayer var1);
}
