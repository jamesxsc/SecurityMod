package uk.co.xsc.securitymod;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class ChunkRenderWorker implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ChunkBatcher batcher;
    private final BlockLayeredBufferBuilder bufferBuilders;
    private boolean running;

    public ChunkRenderWorker(ChunkBatcher chunkBatcher_1) {
        this(chunkBatcher_1, (BlockLayeredBufferBuilder)null);
    }

    public ChunkRenderWorker(ChunkBatcher chunkBatcher_1, BlockLayeredBufferBuilder blockLayeredBufferBuilder_1) {
        this.running = true;
        this.batcher = chunkBatcher_1;
        this.bufferBuilders = blockLayeredBufferBuilder_1;
    }

    public void run() {
        while(this.running) {
            try {
                this.runTask(this.batcher.getNextChunkRenderDataTask());
            } catch (InterruptedException var3) {
                LOGGER.debug("Stopping chunk worker due to interrupt");
                return;
            } catch (Throwable var4) {
                CrashReport crashReport_1 = CrashReport.create(var4, "Batching chunks");
                MinecraftClient.getInstance().setCrashReport(MinecraftClient.getInstance().populateCrashReport(crashReport_1));
                return;
            }
        }

    }

    protected void runTask(final ChunkRenderTask chunkRenderTask_1) throws InterruptedException {
        chunkRenderTask_1.getLock().lock();

        label247: {
            try {
                if (chunkRenderTask_1.getStage() != ChunkRenderTask.Stage.PENDING) {
                    if (!chunkRenderTask_1.isCancelled()) {
                        LOGGER.warn("Chunk render task was {} when I expected it to be pending; ignoring task", chunkRenderTask_1.getStage());
                    }

                    return;
                }

                if (chunkRenderTask_1.getChunkRenderer().shouldBuild()) {
                    chunkRenderTask_1.setStage(ChunkRenderTask.Stage.COMPILING);
                    break label247;
                }

                chunkRenderTask_1.cancel();
            } finally {
                chunkRenderTask_1.getLock().unlock();
            }

            return;
        }

        chunkRenderTask_1.setBufferBuilders(this.getBufferBuilders());
        Vec3d vec3d_1 = this.batcher.getCameraPosition();
        float float_1 = (float)vec3d_1.x;
        float float_2 = (float)vec3d_1.y;
        float float_3 = (float)vec3d_1.z;
        ChunkRenderTask.Mode chunkRenderTask$Mode_1 = chunkRenderTask_1.getMode();
        if (chunkRenderTask$Mode_1 == ChunkRenderTask.Mode.REBUILD_CHUNK) {
            chunkRenderTask_1.getChunkRenderer().rebuildChunk(float_1, float_2, float_3, chunkRenderTask_1);
        } else if (chunkRenderTask$Mode_1 == ChunkRenderTask.Mode.RESORT_TRANSPARENCY) {
            chunkRenderTask_1.getChunkRenderer().resortTransparency(float_1, float_2, float_3, chunkRenderTask_1);
        }

        chunkRenderTask_1.getLock().lock();

        label234: {
            try {
                if (chunkRenderTask_1.getStage() == ChunkRenderTask.Stage.COMPILING) {
                    chunkRenderTask_1.setStage(ChunkRenderTask.Stage.UPLOADING);
                    break label234;
                }

                if (!chunkRenderTask_1.isCancelled()) {
                    LOGGER.warn("Chunk render task was {} when I expected it to be compiling; aborting task", chunkRenderTask_1.getStage());
                }

                this.freeRenderTask(chunkRenderTask_1);
            } finally {
                chunkRenderTask_1.getLock().unlock();
            }

            return;
        }

        final ChunkRenderData chunkRenderData_1 = chunkRenderTask_1.getRenderData();
        ArrayList list_1 = Lists.newArrayList();
        if (chunkRenderTask$Mode_1 == ChunkRenderTask.Mode.REBUILD_CHUNK) {
            BlockRenderLayer[] var9 = BlockRenderLayer.values();
            int var10 = var9.length;

            for(int var11 = 0; var11 < var10; ++var11) {
                BlockRenderLayer blockRenderLayer_1 = var9[var11];
                if (chunkRenderData_1.isBufferInitialized(blockRenderLayer_1)) {
                    list_1.add(this.batcher.upload(blockRenderLayer_1, chunkRenderTask_1.getBufferBuilders().get(blockRenderLayer_1), chunkRenderTask_1.getChunkRenderer(), chunkRenderData_1, chunkRenderTask_1.getSquaredCameraDistance()));
                }
            }
        } else if (chunkRenderTask$Mode_1 == ChunkRenderTask.Mode.RESORT_TRANSPARENCY) {
            list_1.add(this.batcher.upload(BlockRenderLayer.TRANSLUCENT, chunkRenderTask_1.getBufferBuilders().get(BlockRenderLayer.TRANSLUCENT), chunkRenderTask_1.getChunkRenderer(), chunkRenderData_1, chunkRenderTask_1.getSquaredCameraDistance()));
        }

        ListenableFuture<List<Object>> listenableFuture_1 = Futures.allAsList(list_1);
        chunkRenderTask_1.addCompletionAction(() -> {
            listenableFuture_1.cancel(false);
        });
        Futures.addCallback(listenableFuture_1, new FutureCallback<List<Object>>() {
            @Override
            public void onSuccess(List<Object> list_1) {
                ChunkRenderWorker.this.freeRenderTask(chunkRenderTask_1);
                chunkRenderTask_1.getLock().lock();

                try {
                    if (chunkRenderTask_1.getStage() != ChunkRenderTask.Stage.UPLOADING) {
                        if (!chunkRenderTask_1.isCancelled()) {
                            ChunkRenderWorker.LOGGER.warn("Chunk render task was {} when I expected it to be uploading; aborting task", chunkRenderTask_1.getStage());
                        }

                        return;
                    }

                    chunkRenderTask_1.setStage(ChunkRenderTask.Stage.DONE);
                } finally {
                    chunkRenderTask_1.getLock().unlock();
                }

                chunkRenderTask_1.getChunkRenderer().setData(chunkRenderData_1);
            }

            public void onFailure(Throwable throwable_1) {
                ChunkRenderWorker.this.freeRenderTask(chunkRenderTask_1);
                if (!(throwable_1 instanceof CancellationException) && !(throwable_1 instanceof InterruptedException)) {
                    MinecraftClient.getInstance().setCrashReport(CrashReport.create(throwable_1, "Rendering chunk"));
                }

            }
        });
    }

    private BlockLayeredBufferBuilder getBufferBuilders() throws InterruptedException {
        return this.bufferBuilders != null ? this.bufferBuilders : this.batcher.getNextAvailableBuffer();
    }

    private void freeRenderTask(ChunkRenderTask chunkRenderTask_1) {
        if (this.bufferBuilders == null) {
            this.batcher.addAvailableBuffer(chunkRenderTask_1.getBufferBuilders());
        }

    }

    public void stop() {
        this.running = false;
    }
}
