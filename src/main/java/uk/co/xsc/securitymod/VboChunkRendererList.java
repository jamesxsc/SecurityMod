package uk.co.xsc.securitymod;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.gl.GlBuffer;

import java.util.Iterator;

@Environment(EnvType.CLIENT)
public class VboChunkRendererList extends ChunkRendererList {
    public void render(BlockRenderLayer blockRenderLayer_1) {
        if (this.isCameraPositionSet) {
            Iterator var2 = this.chunkRenderers.iterator();

            while(var2.hasNext()) {
                ChunkRenderer chunkRenderer_1 = (ChunkRenderer)var2.next();
                GlBuffer glBuffer_1 = chunkRenderer_1.getGlBuffer(blockRenderLayer_1.ordinal());
                GlStateManager.pushMatrix();
                this.translateToOrigin(chunkRenderer_1);
                glBuffer_1.bind();
                this.method_1356();
                glBuffer_1.draw(7);
                GlStateManager.popMatrix();
            }

            GlBuffer.unbind();
            GlStateManager.clearCurrentColor();
            this.chunkRenderers.clear();
        }
    }

    private void method_1356() {
        GlStateManager.vertexPointer(3, 5126, 28, 0);
        GlStateManager.colorPointer(4, 5121, 28, 12);
        GlStateManager.texCoordPointer(2, 5126, 28, 16);
        GLX.glClientActiveTexture(GLX.GL_TEXTURE1);
        GlStateManager.texCoordPointer(2, 5122, 28, 24);
        GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
    }
}
