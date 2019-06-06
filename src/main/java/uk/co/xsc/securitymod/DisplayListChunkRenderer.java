package uk.co.xsc.securitymod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class DisplayListChunkRenderer extends ChunkRenderer {
    private final int displayListsStartIndex = GlAllocationUtils.genLists(BlockRenderLayer.values().length);

    public DisplayListChunkRenderer(World world_1, SecurityWorldCameraRenderTestRenderer worldRenderer_1) {
        super(world_1, worldRenderer_1);
    }

    public int method_3639(BlockRenderLayer blockRenderLayer_1, ChunkRenderData chunkRenderData_1) {
        return !chunkRenderData_1.isEmpty(blockRenderLayer_1) ? this.displayListsStartIndex + blockRenderLayer_1.ordinal() : -1;
    }

    public void delete() {
        super.delete();
        GlAllocationUtils.deleteLists(this.displayListsStartIndex, BlockRenderLayer.values().length);
    }
}
