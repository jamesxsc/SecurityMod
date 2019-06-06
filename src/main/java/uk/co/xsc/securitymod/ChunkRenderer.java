package uk.co.xsc.securitymod;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GLX;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.SystemUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
public class ChunkRenderer {
    private volatile World world;
    private final SecurityWorldCameraRenderTestRenderer renderer;
    public static int chunkUpdateCount;
    public uk.co.xsc.securitymod.ChunkRenderData data;
    private final ReentrantLock lock;
    private final ReentrantLock dataLock;
    private uk.co.xsc.securitymod.ChunkRenderTask task;
    private final Set<BlockEntity> blockEntities;
    private final GlBuffer[] buffers;
    public BoundingBox boundingBox;
    private int field_4471;
    private boolean rebuildScheduled;
    private final BlockPos.Mutable origin;
    private final BlockPos.Mutable[] neighborPositions;
    private boolean rebuildOnClientThread;

    public ChunkRenderer(World world_1, SecurityWorldCameraRenderTestRenderer worldRenderer_1) {
        this.data = uk.co.xsc.securitymod.ChunkRenderData.EMPTY;
        this.lock = new ReentrantLock();
        this.dataLock = new ReentrantLock();
        this.blockEntities = Sets.newHashSet();
        this.buffers = new GlBuffer[BlockRenderLayer.values().length];
        this.field_4471 = -1;
        this.rebuildScheduled = true;
        this.origin = new BlockPos.Mutable(-1, -1, -1);
        this.neighborPositions = (BlockPos.Mutable[])SystemUtil.consume(new BlockPos.Mutable[6], (blockPos$Mutables_1) -> {
            for(int int_1 = 0; int_1 < blockPos$Mutables_1.length; ++int_1) {
                blockPos$Mutables_1[int_1] = new BlockPos.Mutable();
            }

        });
        this.world = world_1;
        this.renderer = worldRenderer_1;
        if (GLX.useVbo()) {
            for(int int_1 = 0; int_1 < BlockRenderLayer.values().length; ++int_1) {
                this.buffers[int_1] = new GlBuffer(VertexFormats.POSITION_COLOR_UV_LMAP);
            }
        }

    }

    private static boolean isChunkNonEmpty(BlockPos blockPos_1, World world_1) {
        return !world_1.method_8497(blockPos_1.getX() >> 4, blockPos_1.getZ() >> 4).isEmpty();
    }

    public boolean shouldBuild() {
        if (this.getSquaredCameraDistance() <= 576.0D) {
            return true;
        } else {
            World world_1 = this.getWorld();
            return isChunkNonEmpty(this.neighborPositions[Direction.WEST.ordinal()], world_1) && isChunkNonEmpty(this.neighborPositions[Direction.NORTH.ordinal()], world_1) && isChunkNonEmpty(this.neighborPositions[Direction.EAST.ordinal()], world_1) && isChunkNonEmpty(this.neighborPositions[Direction.SOUTH.ordinal()], world_1);
        }
    }

    public boolean method_3671(int int_1) {
        if (this.field_4471 == int_1) {
            return false;
        } else {
            this.field_4471 = int_1;
            return true;
        }
    }

    public GlBuffer getGlBuffer(int int_1) {
        return this.buffers[int_1];
    }

    public void setOrigin(int int_1, int int_2, int int_3) {
        if (int_1 != this.origin.getX() || int_2 != this.origin.getY() || int_3 != this.origin.getZ()) {
            this.clear();
            this.origin.set(int_1, int_2, int_3);
            this.boundingBox = new BoundingBox((double)int_1, (double)int_2, (double)int_3, (double)(int_1 + 16), (double)(int_2 + 16), (double)(int_3 + 16));
            Direction[] var4 = Direction.values();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Direction direction_1 = var4[var6];
                this.neighborPositions[direction_1.ordinal()].set((Vec3i)this.origin).setOffset(direction_1, 16);
            }

        }
    }

    public void resortTransparency(float float_1, float float_2, float float_3, uk.co.xsc.securitymod.ChunkRenderTask chunkRenderTask_1) {
        uk.co.xsc.securitymod.ChunkRenderData chunkRenderData_1 = chunkRenderTask_1.getRenderData();
        if (chunkRenderData_1.getBufferState() != null && !chunkRenderData_1.isEmpty(BlockRenderLayer.TRANSLUCENT)) {
            this.beginBufferBuilding(chunkRenderTask_1.getBufferBuilders().get(BlockRenderLayer.TRANSLUCENT), this.origin);
            chunkRenderTask_1.getBufferBuilders().get(BlockRenderLayer.TRANSLUCENT).restoreState(chunkRenderData_1.getBufferState());
            this.endBufferBuilding(BlockRenderLayer.TRANSLUCENT, float_1, float_2, float_3, chunkRenderTask_1.getBufferBuilders().get(BlockRenderLayer.TRANSLUCENT), chunkRenderData_1);
        }
    }

    public void rebuildChunk(float float_1, float float_2, float float_3, uk.co.xsc.securitymod.ChunkRenderTask chunkRenderTask_1) {
        uk.co.xsc.securitymod.ChunkRenderData chunkRenderData_1 = new uk.co.xsc.securitymod.ChunkRenderData();
        BlockPos blockPos_1 = this.origin.toImmutable();
        BlockPos blockPos_2 = blockPos_1.add(15, 15, 15);
        World world_1 = this.world;
        if (world_1 != null) {
            chunkRenderTask_1.getLock().lock();

            try {
                if (chunkRenderTask_1.getStage() != uk.co.xsc.securitymod.ChunkRenderTask.Stage.COMPILING) {
                    return;
                }

                chunkRenderTask_1.setRenderData(chunkRenderData_1);
            } finally {
                chunkRenderTask_1.getLock().unlock();
            }

            ChunkOcclusionGraphBuilder chunkOcclusionGraphBuilder_1 = new ChunkOcclusionGraphBuilder();
            HashSet set_1 = Sets.newHashSet();
            ChunkRendererRegion chunkRendererRegion_1 = chunkRenderTask_1.takeRegion();
            if (chunkRendererRegion_1 != null) {
                ++chunkUpdateCount;
                boolean[] booleans_1 = new boolean[BlockRenderLayer.values().length];
                BlockModelRenderer.enableBrightnessCache();
                Random random_1 = new Random();
                BlockRenderManager blockRenderManager_1 = MinecraftClient.getInstance().getBlockRenderManager();
                Iterator var16 = BlockPos.iterate(blockPos_1, blockPos_2).iterator();

                while(var16.hasNext()) {
                    BlockPos blockPos_3 = (BlockPos)var16.next();
                    BlockState blockState_1 = chunkRendererRegion_1.getBlockState(blockPos_3);
                    Block block_1 = blockState_1.getBlock();
                    if (blockState_1.isFullOpaque(chunkRendererRegion_1, blockPos_3)) {
                        chunkOcclusionGraphBuilder_1.markClosed(blockPos_3);
                    }

                    if (block_1.hasBlockEntity()) {
                        BlockEntity blockEntity_1 = chunkRendererRegion_1.getBlockEntity(blockPos_3, WorldChunk.CreationType.CHECK);
                        if (blockEntity_1 != null) {
                            BlockEntityRenderer<BlockEntity> blockEntityRenderer_1 = BlockEntityRenderDispatcher.INSTANCE.get(blockEntity_1);
                            if (blockEntityRenderer_1 != null) {
                                chunkRenderData_1.addBlockEntity(blockEntity_1);
                                if (blockEntityRenderer_1.method_3563(blockEntity_1)) {
                                    set_1.add(blockEntity_1);
                                }
                            }
                        }
                    }

                    FluidState fluidState_1 = chunkRendererRegion_1.getFluidState(blockPos_3);
                    int int_3;
                    BufferBuilder bufferBuilder_2;
                    BlockRenderLayer blockRenderLayer_2;
                    if (!fluidState_1.isEmpty()) {
                        blockRenderLayer_2 = fluidState_1.getRenderLayer();
                        int_3 = blockRenderLayer_2.ordinal();
                        bufferBuilder_2 = chunkRenderTask_1.getBufferBuilders().get(int_3);
                        if (!chunkRenderData_1.isBufferInitialized(blockRenderLayer_2)) {
                            chunkRenderData_1.markBufferInitialized(blockRenderLayer_2);
                            this.beginBufferBuilding(bufferBuilder_2, blockPos_1);
                        }

                        booleans_1[int_3] |= blockRenderManager_1.tesselateFluid(blockPos_3, chunkRendererRegion_1, bufferBuilder_2, fluidState_1);
                    }

                    if (blockState_1.getRenderType() != BlockRenderType.INVISIBLE) {
                        blockRenderLayer_2 = block_1.getRenderLayer();
                        int_3 = blockRenderLayer_2.ordinal();
                        bufferBuilder_2 = chunkRenderTask_1.getBufferBuilders().get(int_3);
                        if (!chunkRenderData_1.isBufferInitialized(blockRenderLayer_2)) {
                            chunkRenderData_1.markBufferInitialized(blockRenderLayer_2);
                            this.beginBufferBuilding(bufferBuilder_2, blockPos_1);
                        }

                        booleans_1[int_3] |= blockRenderManager_1.tesselateBlock(blockState_1, blockPos_3, chunkRendererRegion_1, bufferBuilder_2, random_1);
                    }
                }

                BlockRenderLayer[] var33 = BlockRenderLayer.values();
                int var34 = var33.length;

                for(int var35 = 0; var35 < var34; ++var35) {
                    BlockRenderLayer blockRenderLayer_3 = var33[var35];
                    if (booleans_1[blockRenderLayer_3.ordinal()]) {
                        chunkRenderData_1.setNonEmpty(blockRenderLayer_3);
                    }

                    if (chunkRenderData_1.isBufferInitialized(blockRenderLayer_3)) {
                        this.endBufferBuilding(blockRenderLayer_3, float_1, float_2, float_3, chunkRenderTask_1.getBufferBuilders().get(blockRenderLayer_3), chunkRenderData_1);
                    }
                }

                BlockModelRenderer.disableBrightnessCache();
            }

            chunkRenderData_1.setOcclusionGraph(chunkOcclusionGraphBuilder_1.build());
            this.lock.lock();

            try {
                Set<BlockEntity> set_2 = Sets.newHashSet(set_1);
                Set<BlockEntity> set_3 = Sets.newHashSet(this.blockEntities);
                set_2.removeAll(this.blockEntities);
                set_3.removeAll(set_1);
                this.blockEntities.clear();
                this.blockEntities.addAll(set_1);
                this.renderer.updateBlockEntities(set_3, set_2);
            } finally {
                this.lock.unlock();
            }

        }
    }

    protected void cancel() {
        this.lock.lock();

        try {
            if (this.task != null && this.task.getStage() != uk.co.xsc.securitymod.ChunkRenderTask.Stage.DONE) {
                this.task.cancel();
                this.task = null;
            }
        } finally {
            this.lock.unlock();
        }

    }

    public ReentrantLock getLock() {
        return this.lock;
    }

    public uk.co.xsc.securitymod.ChunkRenderTask startRebuild() {
        this.lock.lock();

        uk.co.xsc.securitymod.ChunkRenderTask var4;
        try {
            this.cancel();
            BlockPos blockPos_1 = this.origin.toImmutable();
            ChunkRendererRegion chunkRendererRegion_1 = ChunkRendererRegion.create(this.world, blockPos_1.add(-1, -1, -1), blockPos_1.add(16, 16, 16), 1);
            this.task = new uk.co.xsc.securitymod.ChunkRenderTask(this, uk.co.xsc.securitymod.ChunkRenderTask.Mode.REBUILD_CHUNK, this.getSquaredCameraDistance(), chunkRendererRegion_1);
            var4 = this.task;
        } finally {
            this.lock.unlock();
        }

        return var4;
    }

    public uk.co.xsc.securitymod.ChunkRenderTask startResortTransparency() {
        this.lock.lock();

        uk.co.xsc.securitymod.ChunkRenderTask var1;
        try {
            if (this.task == null || this.task.getStage() != uk.co.xsc.securitymod.ChunkRenderTask.Stage.PENDING) {
                if (this.task != null && this.task.getStage() != uk.co.xsc.securitymod.ChunkRenderTask.Stage.DONE) {
                    this.task.cancel();
                    this.task = null;
                }

                this.task = new uk.co.xsc.securitymod.ChunkRenderTask(this, uk.co.xsc.securitymod.ChunkRenderTask.Mode.RESORT_TRANSPARENCY, this.getSquaredCameraDistance(), (ChunkRendererRegion)null);
                this.task.setRenderData(this.data);
                var1 = this.task;
                return var1;
            }

            var1 = null;
        } finally {
            this.lock.unlock();
        }

        return var1;
    }

    protected double getSquaredCameraDistance() {
        Camera camera_1 = MinecraftClient.getInstance().gameRenderer.getCamera();
        double double_1 = this.boundingBox.minX + 8.0D - camera_1.getPos().x;
        double double_2 = this.boundingBox.minY + 8.0D - camera_1.getPos().y;
        double double_3 = this.boundingBox.minZ + 8.0D - camera_1.getPos().z;
        return double_1 * double_1 + double_2 * double_2 + double_3 * double_3;
    }

    private void beginBufferBuilding(BufferBuilder bufferBuilder_1, BlockPos blockPos_1) {
        bufferBuilder_1.begin(7, VertexFormats.POSITION_COLOR_UV_LMAP);
        bufferBuilder_1.setOffset((double)(-blockPos_1.getX()), (double)(-blockPos_1.getY()), (double)(-blockPos_1.getZ()));
    }

    private void endBufferBuilding(BlockRenderLayer blockRenderLayer_1, float float_1, float float_2, float float_3, BufferBuilder bufferBuilder_1, uk.co.xsc.securitymod.ChunkRenderData chunkRenderData_1) {
        if (blockRenderLayer_1 == BlockRenderLayer.TRANSLUCENT && !chunkRenderData_1.isEmpty(blockRenderLayer_1)) {
            bufferBuilder_1.sortQuads(float_1, float_2, float_3);
            chunkRenderData_1.setBufferState(bufferBuilder_1.toBufferState());
        }

        bufferBuilder_1.end();
    }

    public uk.co.xsc.securitymod.ChunkRenderData getData() {
        return this.data;
    }

    public void setData(uk.co.xsc.securitymod.ChunkRenderData chunkRenderData_1) {
        this.dataLock.lock();

        try {
            this.data = chunkRenderData_1;
        } finally {
            this.dataLock.unlock();
        }

    }

    public void clear() {
        this.cancel();
        this.data = uk.co.xsc.securitymod.ChunkRenderData.EMPTY;
        this.rebuildScheduled = true;
    }

    public void delete() {
        this.clear();
        this.world = null;

        for(int int_1 = 0; int_1 < BlockRenderLayer.values().length; ++int_1) {
            if (this.buffers[int_1] != null) {
                this.buffers[int_1].delete();
            }
        }

    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public void scheduleRebuild(boolean boolean_1) {
        if (this.rebuildScheduled) {
            boolean_1 |= this.rebuildOnClientThread;
        }

        this.rebuildScheduled = true;
        this.rebuildOnClientThread = boolean_1;
    }

    public void unscheduleRebuild() {
        this.rebuildScheduled = false;
        this.rebuildOnClientThread = false;
    }

    public boolean shouldRebuild() {
        return this.rebuildScheduled;
    }

    public boolean shouldRebuildOnClientThread() {
        return this.rebuildScheduled && this.rebuildOnClientThread;
    }

    public BlockPos getNeighborPosition(Direction direction_1) {
        return this.neighborPositions[direction_1.ordinal()];
    }

    public World getWorld() {
        return this.world;
    }
}
