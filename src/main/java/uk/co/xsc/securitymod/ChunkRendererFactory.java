package uk.co.xsc.securitymod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public interface ChunkRendererFactory {
    uk.co.xsc.securitymod.ChunkRenderer create(World var1, SecurityWorldCameraRenderTestRenderer var2);
}
