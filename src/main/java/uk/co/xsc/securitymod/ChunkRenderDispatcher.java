package uk.co.xsc.securitymod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class ChunkRenderDispatcher {
    protected final SecurityWorldCameraRenderTestRenderer renderer;
    protected final World world;
    protected int sizeY;
    protected int sizeX;
    protected int sizeZ;
    public uk.co.xsc.securitymod.ChunkRenderer[] renderers;

    public ChunkRenderDispatcher(World world_1, int int_1, SecurityWorldCameraRenderTestRenderer worldRenderer_1, uk.co.xsc.securitymod.ChunkRendererFactory chunkRendererFactory_1) {
        this.renderer = worldRenderer_1;
        this.world = world_1;
        this.method_3325(int_1);
        this.createChunks(chunkRendererFactory_1);
    }

    protected void createChunks(uk.co.xsc.securitymod.ChunkRendererFactory chunkRendererFactory_1) {
        int int_1 = this.sizeX * this.sizeY * this.sizeZ;
        this.renderers = new uk.co.xsc.securitymod.ChunkRenderer[int_1];

        for(int int_2 = 0; int_2 < this.sizeX; ++int_2) {
            for(int int_3 = 0; int_3 < this.sizeY; ++int_3) {
                for(int int_4 = 0; int_4 < this.sizeZ; ++int_4) {
                    int int_5 = this.getChunkIndex(int_2, int_3, int_4);
                    this.renderers[int_5] = chunkRendererFactory_1.create(this.world, this.renderer);
                    this.renderers[int_5].setOrigin(int_2 * 16, int_3 * 16, int_4 * 16);
                }
            }
        }

    }

    public void delete() {
        uk.co.xsc.securitymod.ChunkRenderer[] var1 = this.renderers;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1 = var1[var3];
            chunkRenderer_1.delete();
        }

    }

    private int getChunkIndex(int int_1, int int_2, int int_3) {
        return (int_3 * this.sizeY + int_2) * this.sizeX + int_1;
    }

    protected void method_3325(int int_1) {
        int int_2 = int_1 * 2 + 1;
        this.sizeX = int_2;
        this.sizeY = 16;
        this.sizeZ = int_2;
    }

    public void updateCameraPosition(double double_1, double double_2) {
        int int_1 = MathHelper.floor(double_1) - 8;
        int int_2 = MathHelper.floor(double_2) - 8;
        int int_3 = this.sizeX * 16;

        for(int int_4 = 0; int_4 < this.sizeX; ++int_4) {
            int int_5 = this.method_3328(int_1, int_3, int_4);

            for(int int_6 = 0; int_6 < this.sizeZ; ++int_6) {
                int int_7 = this.method_3328(int_2, int_3, int_6);

                for(int int_8 = 0; int_8 < this.sizeY; ++int_8) {
                    int int_9 = int_8 * 16;
                    uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1 = this.renderers[this.getChunkIndex(int_4, int_8, int_6)];
                    chunkRenderer_1.setOrigin(int_5, int_9, int_7);
                }
            }
        }

    }

    private int method_3328(int int_1, int int_2, int int_3) {
        int int_4 = int_3 * 16;
        int int_5 = int_4 - int_1 + int_2 / 2;
        if (int_5 < 0) {
            int_5 -= int_2 - 1;
        }

        return int_4 - int_5 / int_2 * int_2;
    }

    public void scheduleChunkRender(int int_1, int int_2, int int_3, boolean boolean_1) {
        int int_4 = Math.floorMod(int_1, this.sizeX);
        int int_5 = Math.floorMod(int_2, this.sizeY);
        int int_6 = Math.floorMod(int_3, this.sizeZ);
        uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1 = this.renderers[this.getChunkIndex(int_4, int_5, int_6)];
        chunkRenderer_1.scheduleRebuild(boolean_1);
    }

    protected uk.co.xsc.securitymod.ChunkRenderer getChunkRenderer(BlockPos blockPos_1) {
        int int_1 = MathHelper.floorDiv(blockPos_1.getX(), 16);
        int int_2 = MathHelper.floorDiv(blockPos_1.getY(), 16);
        int int_3 = MathHelper.floorDiv(blockPos_1.getZ(), 16);
        if (int_2 >= 0 && int_2 < this.sizeY) {
            int_1 = MathHelper.floorMod(int_1, this.sizeX);
            int_3 = MathHelper.floorMod(int_3, this.sizeZ);
            return this.renderers[this.getChunkIndex(int_1, int_2, int_3)];
        } else {
            return null;
        }
    }
}
