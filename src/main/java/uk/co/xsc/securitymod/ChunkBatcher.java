package uk.co.xsc.securitymod;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBuffer;
import net.minecraft.client.gl.GlBufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.util.SystemUtil;
import net.minecraft.util.UncaughtExceptionLogger;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;

@Environment(EnvType.CLIENT)
public class ChunkBatcher {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadFactory THREAD_FACTORY;

    static {
        THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Chunk Batcher %d").setDaemon(true).setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER)).build();
    }

    private final int bufferCount;
    private final List<Thread> workerThreads = Lists.newArrayList();
    private final List<ChunkRenderWorker> workers = Lists.newArrayList();
    private final PriorityBlockingQueue<ChunkRenderTask> pendingChunks = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<BlockLayeredBufferBuilder> availableBuffers;
    private final BufferRenderer displayListBufferRenderer = new BufferRenderer();
    private final GlBufferRenderer vboBufferRenderer = new GlBufferRenderer();
    private final Queue<ChunkBatcher.ChunkUploadTask> pendingUploads = Queues.newPriorityQueue();
    private final uk.co.xsc.securitymod.ChunkRenderWorker clientThreadWorker;
    private Vec3d cameraPosition;

    public ChunkBatcher(boolean boolean_1) {
        this.cameraPosition = Vec3d.ZERO;
        int int_1 = Math.max(1, (int) ((double) Runtime.getRuntime().maxMemory() * 0.3D) / 10485760 - 1);
        int int_2 = Runtime.getRuntime().availableProcessors();
        int int_3 = boolean_1 ? int_2 : Math.min(int_2, 4);
        int int_4 = Math.max(1, Math.min(int_3 * 3, int_1));
        this.clientThreadWorker = new ChunkRenderWorker(this, new BlockLayeredBufferBuilder());
        ArrayList list_1 = Lists.newArrayListWithExpectedSize(int_4);

        int int_8;
        int int_9;
        try {
            for (int_8 = 0; int_8 < int_4; ++int_8) {
                list_1.add(new BlockLayeredBufferBuilder());
            }
        } catch (OutOfMemoryError var11) {
            LOGGER.warn("Allocated only {}/{} buffers", list_1.size(), int_4);
            int_9 = list_1.size() * 2 / 3;

            for (int int_7 = 0; int_7 < int_9; ++int_7) {
                list_1.remove(list_1.size() - 1);
            }

            System.gc();
        }

        this.bufferCount = list_1.size();
        this.availableBuffers = Queues.newArrayBlockingQueue(this.bufferCount);
        this.availableBuffers.addAll(list_1);
        int_8 = Math.min(int_3, this.bufferCount);
        if (int_8 > 1) {
            for (int_9 = 0; int_9 < int_8; ++int_9) {
                ChunkRenderWorker chunkRenderWorker_1 = new ChunkRenderWorker(this);
                Thread thread_1 = THREAD_FACTORY.newThread(chunkRenderWorker_1);
                thread_1.start();
                this.workers.add(chunkRenderWorker_1);
                this.workerThreads.add(thread_1);
            }
        }

    }

    public String getDebugString() {
        return this.workerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.pendingChunks.size()) : String.format("pC: %03d, pU: %02d, aB: %02d", this.pendingChunks.size(), this.pendingUploads.size(), this.availableBuffers.size());
    }

    public Vec3d getCameraPosition() {
        return this.cameraPosition;
    }

    public void setCameraPosition(Vec3d vec3d_1) {
        this.cameraPosition = vec3d_1;
    }

    public boolean runTasksSync(long long_1) {
        boolean boolean_1 = false;

        boolean boolean_2;
        do {
            boolean_2 = false;
            if (this.workerThreads.isEmpty()) {
                ChunkRenderTask chunkRenderTask_1 = (ChunkRenderTask) this.pendingChunks.poll();
                if (chunkRenderTask_1 != null) {
                    try {
                        this.clientThreadWorker.runTask(chunkRenderTask_1);
                        boolean_2 = true;
                    } catch (InterruptedException var8) {
                        LOGGER.warn("Skipped task due to interrupt");
                    }
                }
            }

            synchronized (this.pendingUploads) {
                if (!this.pendingUploads.isEmpty()) {
                    ((ChunkBatcher.ChunkUploadTask) this.pendingUploads.poll()).task.run();
                    boolean_2 = true;
                    boolean_1 = true;
                }
            }
        } while (long_1 != 0L && boolean_2 && long_1 >= SystemUtil.getMeasuringTimeNano());

        return boolean_1;
    }

    public boolean rebuild(uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1) {
        chunkRenderer_1.getLock().lock();

        boolean var4;
        try {
            uk.co.xsc.securitymod.ChunkRenderTask chunkRenderTask_1 = chunkRenderer_1.startRebuild();
            chunkRenderTask_1.addCompletionAction(() -> {
                this.pendingChunks.remove(chunkRenderTask_1);
            });
            boolean boolean_1 = this.pendingChunks.offer(chunkRenderTask_1);
            if (!boolean_1) {
                chunkRenderTask_1.cancel();
            }

            var4 = boolean_1;
        } finally {
            chunkRenderer_1.getLock().unlock();
        }

        return var4;
    }

    public boolean rebuildSync(uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1) {
        chunkRenderer_1.getLock().lock();

        boolean var3;
        try {
            ChunkRenderTask chunkRenderTask_1 = chunkRenderer_1.startRebuild();

            try {
                this.clientThreadWorker.runTask(chunkRenderTask_1);
            } catch (InterruptedException var7) {
            }

            var3 = true;
        } finally {
            chunkRenderer_1.getLock().unlock();
        }

        return var3;
    }

    public void reset() {
        this.clear();
        ArrayList list_1 = Lists.newArrayList();

        while (list_1.size() != this.bufferCount) {
            this.runTasksSync(Long.MAX_VALUE);

            try {
                list_1.add(this.getNextAvailableBuffer());
            } catch (InterruptedException var3) {
            }
        }

        this.availableBuffers.addAll(list_1);
    }

    public void addAvailableBuffer(BlockLayeredBufferBuilder blockLayeredBufferBuilder_1) {
        this.availableBuffers.add(blockLayeredBufferBuilder_1);
    }

    public BlockLayeredBufferBuilder getNextAvailableBuffer() throws InterruptedException {
        return (BlockLayeredBufferBuilder) this.availableBuffers.take();
    }

    public ChunkRenderTask getNextChunkRenderDataTask() throws InterruptedException {
        return (ChunkRenderTask) this.pendingChunks.take();
    }

    public boolean resortTransparency(ChunkRenderer chunkRenderer_1) {
        chunkRenderer_1.getLock().lock();

        boolean var3;
        try {
            ChunkRenderTask chunkRenderTask_1 = chunkRenderer_1.startResortTransparency();
            if (chunkRenderTask_1 != null) {
                chunkRenderTask_1.addCompletionAction(() -> {
                    this.pendingChunks.remove(chunkRenderTask_1);
                });
                var3 = this.pendingChunks.offer(chunkRenderTask_1);
                return var3;
            }

            var3 = true;
        } finally {
            chunkRenderer_1.getLock().unlock();
        }

        return var3;
    }

    public ListenableFuture<Object> upload(BlockRenderLayer blockRenderLayer_1, BufferBuilder bufferBuilder_1, ChunkRenderer chunkRenderer_1, ChunkRenderData chunkRenderData_1, double double_1) {
        if (MinecraftClient.getInstance().isOnThread()) {
            if (GLX.useVbo()) {
                this.uploadVbo(bufferBuilder_1, chunkRenderer_1.getGlBuffer(blockRenderLayer_1.ordinal()));
            } else {
                this.uploadDisplayList(bufferBuilder_1, ((DisplayListChunkRenderer) chunkRenderer_1).method_3639(blockRenderLayer_1, chunkRenderData_1));
            }

            bufferBuilder_1.setOffset(0.0D, 0.0D, 0.0D);
            return Futures.immediateFuture((Object) null);
        } else {
            ListenableFutureTask<Object> listenableFutureTask_1 = ListenableFutureTask.create(() -> {
                this.upload(blockRenderLayer_1, bufferBuilder_1, chunkRenderer_1, chunkRenderData_1, double_1);
            }, (Object) null);
            synchronized (this.pendingUploads) {
                this.pendingUploads.add(new ChunkBatcher.ChunkUploadTask(listenableFutureTask_1, double_1));
                return listenableFutureTask_1;
            }
        }
    }

    private void uploadDisplayList(BufferBuilder bufferBuilder_1, int int_1) {
        GlStateManager.newList(int_1, 4864);
        this.displayListBufferRenderer.draw(bufferBuilder_1);
        GlStateManager.endList();
    }

    private void uploadVbo(BufferBuilder bufferBuilder_1, GlBuffer glBuffer_1) {
        this.vboBufferRenderer.setGlBuffer(glBuffer_1);
        this.vboBufferRenderer.draw(bufferBuilder_1);
    }

    public void clear() {
        while (!this.pendingChunks.isEmpty()) {
            ChunkRenderTask chunkRenderTask_1 = (ChunkRenderTask) this.pendingChunks.poll();
            if (chunkRenderTask_1 != null) {
                chunkRenderTask_1.cancel();
            }
        }

    }

    public boolean isEmpty() {
        return this.pendingChunks.isEmpty() && this.pendingUploads.isEmpty();
    }

    public void stop() {
        this.clear();
        Iterator var1 = this.workers.iterator();

        while (var1.hasNext()) {
            ChunkRenderWorker chunkRenderWorker_1 = (ChunkRenderWorker) var1.next();
            chunkRenderWorker_1.stop();
        }

        var1 = this.workerThreads.iterator();

        while (var1.hasNext()) {
            Thread thread_1 = (Thread) var1.next();

            try {
                thread_1.interrupt();
                thread_1.join();
            } catch (InterruptedException var4) {
                LOGGER.warn("Interrupted whilst waiting for worker to die", var4);
            }
        }

        this.availableBuffers.clear();
    }

    @Environment(EnvType.CLIENT)
    class ChunkUploadTask implements Comparable<ChunkBatcher.ChunkUploadTask> {
        private final ListenableFutureTask<Object> task;
        private final double priority;

        public ChunkUploadTask(ListenableFutureTask<Object> listenableFutureTask_1, double double_1) {
            this.task = listenableFutureTask_1;
            this.priority = double_1;
        }

        public int compareTo(ChunkBatcher.ChunkUploadTask chunkBatcher$ChunkUploadTask_1) {
            return Doubles.compare(this.priority, chunkBatcher$ChunkUploadTask_1.priority);
        }

        // $FF: synthetic method
    }
}
