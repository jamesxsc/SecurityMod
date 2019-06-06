package uk.co.xsc.securitymod;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;

@Environment(EnvType.CLIENT)
public class ChunkRenderTask implements Comparable<ChunkRenderTask> {
    private final ChunkRenderer chunkRenderer;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Runnable> completionActions = Lists.newArrayList();
    private final ChunkRenderTask.Mode mode;
    private final double squaredCameraDistance;
    private ChunkRendererRegion region;
    private BlockLayeredBufferBuilder bufferBuilder;
    private ChunkRenderData renderData;
    private ChunkRenderTask.Stage stage;
    private boolean cancelled;

    public ChunkRenderTask(ChunkRenderer chunkRenderer_1, ChunkRenderTask.Mode chunkRenderTask$Mode_1, double double_1, ChunkRendererRegion chunkRendererRegion_1) {
        this.stage = ChunkRenderTask.Stage.PENDING;
        this.chunkRenderer = chunkRenderer_1;
        this.mode = chunkRenderTask$Mode_1;
        this.squaredCameraDistance = double_1;
        this.region = chunkRendererRegion_1;
    }

    public ChunkRenderTask.Stage getStage() {
        return this.stage;
    }

    public ChunkRenderer getChunkRenderer() {
        return this.chunkRenderer;
    }

    public ChunkRendererRegion takeRegion() {
        ChunkRendererRegion chunkRendererRegion_1 = this.region;
        this.region = null;
        return chunkRendererRegion_1;
    }

    public ChunkRenderData getRenderData() {
        return this.renderData;
    }

    public void setRenderData(ChunkRenderData chunkRenderData_1) {
        this.renderData = chunkRenderData_1;
    }

    public BlockLayeredBufferBuilder getBufferBuilders() {
        return this.bufferBuilder;
    }

    public void setBufferBuilders(BlockLayeredBufferBuilder blockLayeredBufferBuilder_1) {
        this.bufferBuilder = blockLayeredBufferBuilder_1;
    }

    public void setStage(ChunkRenderTask.Stage chunkRenderTask$Stage_1) {
        this.lock.lock();

        try {
            this.stage = chunkRenderTask$Stage_1;
        } finally {
            this.lock.unlock();
        }

    }

    public void cancel() {
        this.lock.lock();

        try {
            this.region = null;
            if (this.mode == ChunkRenderTask.Mode.REBUILD_CHUNK && this.stage != ChunkRenderTask.Stage.DONE) {
                this.chunkRenderer.scheduleRebuild(false);
            }

            this.cancelled = true;
            this.stage = ChunkRenderTask.Stage.DONE;
            Iterator var1 = this.completionActions.iterator();

            while(var1.hasNext()) {
                Runnable runnable_1 = (Runnable)var1.next();
                runnable_1.run();
            }
        } finally {
            this.lock.unlock();
        }

    }

    public void addCompletionAction(Runnable runnable_1) {
        this.lock.lock();

        try {
            this.completionActions.add(runnable_1);
            if (this.cancelled) {
                runnable_1.run();
            }
        } finally {
            this.lock.unlock();
        }

    }

    public ReentrantLock getLock() {
        return this.lock;
    }

    public ChunkRenderTask.Mode getMode() {
        return this.mode;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public int method_3601(ChunkRenderTask chunkRenderTask_1) {
        return Doubles.compare(this.squaredCameraDistance, chunkRenderTask_1.squaredCameraDistance);
    }

    public double getSquaredCameraDistance() {
        return this.squaredCameraDistance;
    }

    // $FF: synthetic method
    @Override
    public int compareTo(ChunkRenderTask o) {
        return this.method_3601(o);
    }

    @Environment(EnvType.CLIENT)
    public static enum Stage {
        PENDING,
        COMPILING,
        UPLOADING,
        DONE;
    }

    @Environment(EnvType.CLIENT)
    public static enum Mode {
        REBUILD_CHUNK,
        RESORT_TRANSPARENCY;
    }
}
