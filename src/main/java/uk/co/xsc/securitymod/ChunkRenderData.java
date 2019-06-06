package uk.co.xsc.securitymod;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionGraph;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class ChunkRenderData {
    public static final ChunkRenderData EMPTY = new ChunkRenderData() {
        protected void setNonEmpty(BlockRenderLayer blockRenderLayer_1) {
            throw new UnsupportedOperationException();
        }

        public void markBufferInitialized(BlockRenderLayer blockRenderLayer_1) {
            throw new UnsupportedOperationException();
        }

        public boolean isVisibleThrough(Direction direction_1, Direction direction_2) {
            return false;
        }
    };
    private final boolean[] nonEmpty = new boolean[BlockRenderLayer.values().length];
    private final boolean[] initialized = new boolean[BlockRenderLayer.values().length];
    private boolean empty = true;
    private final List<BlockEntity> blockEntities = Lists.newArrayList();
    private ChunkOcclusionGraph occlusionGraph = new ChunkOcclusionGraph();
    private BufferBuilder.State bufferState;

    public boolean isEmpty() {
        return this.empty;
    }

    protected void setNonEmpty(BlockRenderLayer blockRenderLayer_1) {
        this.empty = false;
        this.nonEmpty[blockRenderLayer_1.ordinal()] = true;
    }

    public boolean isEmpty(BlockRenderLayer blockRenderLayer_1) {
        return !this.nonEmpty[blockRenderLayer_1.ordinal()];
    }

    public void markBufferInitialized(BlockRenderLayer blockRenderLayer_1) {
        this.initialized[blockRenderLayer_1.ordinal()] = true;
    }

    public boolean isBufferInitialized(BlockRenderLayer blockRenderLayer_1) {
        return this.initialized[blockRenderLayer_1.ordinal()];
    }

    public List<BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addBlockEntity(BlockEntity blockEntity_1) {
        this.blockEntities.add(blockEntity_1);
    }

    public boolean isVisibleThrough(Direction direction_1, Direction direction_2) {
        return this.occlusionGraph.isVisibleThrough(direction_1, direction_2);
    }

    public void setOcclusionGraph(ChunkOcclusionGraph chunkOcclusionGraph_1) {
        this.occlusionGraph = chunkOcclusionGraph_1;
    }

    public BufferBuilder.State getBufferState() {
        return this.bufferState;
    }

    public void setBufferState(BufferBuilder.State bufferBuilder$State_1) {
        this.bufferState = bufferBuilder$State_1;
    }
}
