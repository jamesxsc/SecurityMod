package uk.co.xsc.securitymod;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;

@Environment(EnvType.CLIENT)
public class DisplayListChunkRendererList extends ChunkRendererList {
    public void render(BlockRenderLayer blockRenderLayer_1) {
        if (this.isCameraPositionSet) {
            Iterator var2 = this.chunkRenderers.iterator();

            while(var2.hasNext()) {
                ChunkRenderer chunkRenderer_1 = (ChunkRenderer)var2.next();
                DisplayListChunkRenderer displayListChunkRenderer_1 = (DisplayListChunkRenderer)chunkRenderer_1;
                GlStateManager.pushMatrix();
                this.translateToOrigin(chunkRenderer_1);
                GlStateManager.callList(displayListChunkRenderer_1.method_3639(blockRenderLayer_1, displayListChunkRenderer_1.getData()));
                GlStateManager.popMatrix();
            }

            GlStateManager.clearCurrentColor();
            this.chunkRenderers.clear();
        }
    }
}
