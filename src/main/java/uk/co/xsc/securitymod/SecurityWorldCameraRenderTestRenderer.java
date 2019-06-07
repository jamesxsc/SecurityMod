package uk.co.xsc.securitymod;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.sun.istack.internal.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBuffer;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.gl.GlProgramManager;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.ParticlesOption;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.SystemUtil;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLXARBCreateContext;
import sun.java2d.opengl.OGLContext;

import java.io.IOException;
import java.util.*;

@Environment(EnvType.CLIENT)
public class SecurityWorldCameraRenderTestRenderer implements AutoCloseable, SynchronousResourceReloadListener {
    public static final Direction[] DIRECTIONS = Direction.values();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Identifier MOON_PHASES_TEX = new Identifier("textures/environment/moon_phases.png");
    private static final Identifier SUN_TEX = new Identifier("textures/environment/sun.png");
    private static final Identifier CLOUDS_TEX = new Identifier("textures/environment/clouds.png");
    private static final Identifier END_SKY_TEX = new Identifier("textures/environment/end_sky.png");
    private static final Identifier FORCEFIELD_TEX = new Identifier("textures/misc/forcefield.png");
    private final MinecraftClient client;
    private final TextureManager textureManager;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final GlFramebuffer frameBuffer;
    private final Set<BlockEntity> blockEntities = Sets.newHashSet();
    private final VertexFormat field_4100;
    private final int field_4079 = 28;
    private final Map<Integer, PartiallyBrokenBlockEntry> partiallyBrokenBlocks = Maps.newHashMap();
    private final Map<BlockPos, SoundInstance> playingSongs = Maps.newHashMap();
    private final Sprite[] destroyStages = new Sprite[10];
    private final Vector4f[] field_4065;
    private final Vec3d forcedFrustumPosition;
    private ClientWorld world;
    private Set<uk.co.xsc.securitymod.ChunkRenderer> chunkRenderers = Sets.newLinkedHashSet();
    private List<ChunkInfo> chunkInfos = Lists.newArrayListWithCapacity(69696);
    private ChunkRenderDispatcher chunkRenderDispatcher;
    private int starsDisplayList = -1;
    private int field_4117 = -1;
    private int field_4067 = -1;
    private GlBuffer starsBuffer;
    private GlBuffer field_4087;
    private GlBuffer field_4102;
    private boolean cloudsDirty = true;
    private int cloudsDisplayList = -1;
    private GlBuffer cloudsBuffer;
    private int ticks;
    private GlFramebuffer entityOutlinesFramebuffer;
    private ShaderEffect entityOutlineShader;
    private double lastCameraChunkUpdateX = Double.MIN_VALUE;
    private double lastCameraChunkUpdateY = Double.MIN_VALUE;
    private double lastCameraChunkUpdateZ = Double.MIN_VALUE;
    private int cameraChunkX = Integer.MIN_VALUE;
    private int cameraChunkY = Integer.MIN_VALUE;
    private int cameraChunkZ = Integer.MIN_VALUE;
    private double lastCameraX = Double.MIN_VALUE;
    private double lastCameraY = Double.MIN_VALUE;
    private double lastCameraZ = Double.MIN_VALUE;
    private double lastCameraPitch = Double.MIN_VALUE;
    private double lastCameraYaw = Double.MIN_VALUE;
    private int field_4082 = Integer.MIN_VALUE;
    private int field_4097 = Integer.MIN_VALUE;
    private int field_4116 = Integer.MIN_VALUE;
    private net.minecraft.util.math.Vec3d field_4072;
    private CloudRenderMode field_4080;
    private uk.co.xsc.securitymod.ChunkBatcher chunkBatcher;
    private uk.co.xsc.securitymod.ChunkRendererList chunkRendererList;
    private int renderDistance;
    private int field_4076;
    private int regularEntityCount;
    private int blockEntityCount;
    private boolean field_4066;
    private Frustum forcedFrustum;
    private boolean vertexBufferObjectsEnabled;
    private uk.co.xsc.securitymod.ChunkRendererFactory chunkRendererFactory;
    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean terrainUpdateNecessary;
    private boolean entityOutlinesUpdateNecessary;

    public SecurityWorldCameraRenderTestRenderer() {
        this.field_4072 = net.minecraft.util.math.Vec3d.ZERO;
        this.renderDistance = -1;
        this.field_4076 = 2;
        this.field_4065 = new Vector4f[8];
        this.forcedFrustumPosition = new Vec3d();
        this.terrainUpdateNecessary = true;
        this.client = MinecraftClient.getInstance();
        List<String> list_1 = Lists.newArrayList();
        GLFWErrorCallback gLFWErrorCallback_1 = GLFW.glfwSetErrorCallback((int_1, long_1) -> {
            list_1.add(String.format("GLFW error during init: [0x%X]%s", int_1, long_1));
        });
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join(list_1));
        }
        System.out.println("Calling my glfw");
        long glfwWindow = GLFW.glfwCreateWindow(800, 800, "Test Camera", 0, 0);
        GLFW.glfwMakeContextCurrent(glfwWindow);
        GLFW.glfwShowWindow(glfwWindow);
        this.frameBuffer = null;//new GlFramebuffer(800, 800, true, SystemUtil.getOperatingSystem() == SystemUtil.OperatingSystem.MAC);
        this.entityRenderDispatcher = client.getEntityRenderManager();
        this.textureManager = client.getTextureManager();
        this.vertexBufferObjectsEnabled = GLX.useVbo();
        if (this.vertexBufferObjectsEnabled) {
            this.chunkRendererList = new uk.co.xsc.securitymod.VboChunkRendererList();
            this.chunkRendererFactory = uk.co.xsc.securitymod.ChunkRenderer::new;
        } else {
            this.chunkRendererList = new DisplayListChunkRendererList();
            this.chunkRendererFactory = uk.co.xsc.securitymod.DisplayListChunkRenderer::new;
        }

        this.field_4100 = new VertexFormat();
        this.field_4100.add(new VertexFormatElement(0, VertexFormatElement.Format.FLOAT, VertexFormatElement.Type.POSITION, 3));
        this.renderStars();
        this.method_3277();
        this.method_3265();
    }

    public static void drawDebugShapeOutline(VoxelShape voxelShape_1, double double_1, double double_2, double double_3, float float_1, float float_2, float float_3, float float_4) {
        List<BoundingBox> list_1 = voxelShape_1.getBoundingBoxes();
        int int_1 = MathHelper.ceil((double) list_1.size() / 3.0D);

        for (int int_2 = 0; int_2 < list_1.size(); ++int_2) {
            BoundingBox boundingBox_1 = (BoundingBox) list_1.get(int_2);
            float float_5 = ((float) int_2 % (float) int_1 + 1.0F) / (float) int_1;
            float float_6 = (float) (int_2 / int_1);
            float float_7 = float_5 * (float) (float_6 == 0.0F ? 1 : 0);
            float float_8 = float_5 * (float) (float_6 == 1.0F ? 1 : 0);
            float float_9 = float_5 * (float) (float_6 == 2.0F ? 1 : 0);
            drawShapeOutline(VoxelShapes.cuboid(boundingBox_1.offset(0.0D, 0.0D, 0.0D)), double_1, double_2, double_3, float_7, float_8, float_9, 1.0F);
        }

    }

    public static void drawShapeOutline(VoxelShape voxelShape_1, double double_1, double double_2, double double_3, float float_1, float float_2, float float_3, float float_4) {
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        bufferBuilder_1.begin(1, VertexFormats.POSITION_COLOR);
        voxelShape_1.forEachEdge((double_4, double_5, double_6, double_7, double_8, double_9) -> {
            bufferBuilder_1.vertex(double_4 + double_1, double_5 + double_2, double_6 + double_3).color(float_1, float_2, float_3, float_4).next();
            bufferBuilder_1.vertex(double_7 + double_1, double_8 + double_2, double_9 + double_3).color(float_1, float_2, float_3, float_4).next();
        });
        tessellator_1.draw();
    }

    public static void drawBoxOutline(BoundingBox boundingBox_1, float float_1, float float_2, float float_3, float float_4) {
        drawBoxOutline(boundingBox_1.minX, boundingBox_1.minY, boundingBox_1.minZ, boundingBox_1.maxX, boundingBox_1.maxY, boundingBox_1.maxZ, float_1, float_2, float_3, float_4);
    }

    public static void drawBoxOutline(double double_1, double double_2, double double_3, double double_4, double double_5, double double_6, float float_1, float float_2, float float_3, float float_4) {
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        bufferBuilder_1.begin(3, VertexFormats.POSITION_COLOR);
        buildBoxOutline(bufferBuilder_1, double_1, double_2, double_3, double_4, double_5, double_6, float_1, float_2, float_3, float_4);
        tessellator_1.draw();
    }

    public static void buildBoxOutline(BufferBuilder bufferBuilder_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6, float float_1, float float_2, float float_3, float float_4) {
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, 0.0F).next();
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_6).color(float_1, float_2, float_3, 0.0F).next();
        bufferBuilder_1.vertex(double_1, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, 0.0F).next();
        bufferBuilder_1.vertex(double_4, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_3).color(float_1, float_2, float_3, 0.0F).next();
        bufferBuilder_1.vertex(double_4, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_3).color(float_1, float_2, float_3, 0.0F).next();
    }

    public static void buildBox(BufferBuilder bufferBuilder_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6, float float_1, float float_2, float float_3, float float_4) {
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_2, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_1, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_3).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
        bufferBuilder_1.vertex(double_4, double_5, double_6).color(float_1, float_2, float_3, float_4).next();
    }

    public void close() {
        if (this.entityOutlineShader != null) {
            this.entityOutlineShader.close();
        }

    }

    public void apply(ResourceManager resourceManager_1) {
        this.textureManager.bindTexture(FORCEFIELD_TEX);
        GlStateManager.texParameter(3553, 10242, 10497);
        GlStateManager.texParameter(3553, 10243, 10497);
        GlStateManager.bindTexture(0);
        this.loadDestroyStageTextures();
        this.loadEntityOutlineShader();
    }

    private void loadDestroyStageTextures() {
        SpriteAtlasTexture spriteAtlasTexture_1 = this.client.getSpriteAtlas();
        this.destroyStages[0] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_0);
        this.destroyStages[1] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_1);
        this.destroyStages[2] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_2);
        this.destroyStages[3] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_3);
        this.destroyStages[4] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_4);
        this.destroyStages[5] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_5);
        this.destroyStages[6] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_6);
        this.destroyStages[7] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_7);
        this.destroyStages[8] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_8);
        this.destroyStages[9] = spriteAtlasTexture_1.getSprite(ModelLoader.DESTROY_STAGE_9);
    }

    public void loadEntityOutlineShader() {
        if (GLX.usePostProcess) {
            if (GlProgramManager.getInstance() == null) {
                GlProgramManager.init();
            }

            if (this.entityOutlineShader != null) {
                this.entityOutlineShader.close();
            }

            Identifier identifier_1 = new Identifier("shaders/post/entity_outline.json");

            try {
                this.entityOutlineShader = new ShaderEffect(this.client.getTextureManager(), this.client.getResourceManager(), this.frameBuffer, identifier_1);
                this.entityOutlineShader.setupDimensions(this.client.window.getFramebufferWidth(), this.client.window.getFramebufferHeight());
                this.entityOutlinesFramebuffer = this.entityOutlineShader.getSecondaryTarget("final");
            } catch (IOException var3) {
                LOGGER.warn("Failed to load shader: {}", identifier_1, var3);
                this.entityOutlineShader = null;
                this.entityOutlinesFramebuffer = null;
            } catch (JsonSyntaxException var4) {
                LOGGER.warn("Failed to load shader: {}", identifier_1, var4);
                this.entityOutlineShader = null;
                this.entityOutlinesFramebuffer = null;
            }
        } else {
            this.entityOutlineShader = null;
            this.entityOutlinesFramebuffer = null;
        }

    }

    public void drawEntityOutlinesFramebuffer() {
        if (this.canDrawEntityOutlines()) {
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            this.entityOutlinesFramebuffer.draw(this.client.window.getFramebufferWidth(), this.client.window.getFramebufferHeight(), false);
            GlStateManager.disableBlend();
        }

    }

    protected boolean canDrawEntityOutlines() {
        return this.entityOutlinesFramebuffer != null && this.entityOutlineShader != null && this.client.player != null;
    }

    private void method_3265() {
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        if (this.field_4102 != null) {
            this.field_4102.delete();
        }

        if (this.field_4067 >= 0) {
            GlAllocationUtils.deleteSingletonList(this.field_4067);
            this.field_4067 = -1;
        }

        if (this.vertexBufferObjectsEnabled) {
            this.field_4102 = new GlBuffer(this.field_4100);
            this.method_3283(bufferBuilder_1, -16.0F, true);
            bufferBuilder_1.end();
            bufferBuilder_1.clear();
            this.field_4102.set(bufferBuilder_1.getByteBuffer());
        } else {
            this.field_4067 = GlAllocationUtils.genLists(1);
            GlStateManager.newList(this.field_4067, 4864);
            this.method_3283(bufferBuilder_1, -16.0F, true);
            tessellator_1.draw();
            GlStateManager.endList();
        }

    }

    private void method_3277() {
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        if (this.field_4087 != null) {
            this.field_4087.delete();
        }

        if (this.field_4117 >= 0) {
            GlAllocationUtils.deleteSingletonList(this.field_4117);
            this.field_4117 = -1;
        }

        if (this.vertexBufferObjectsEnabled) {
            this.field_4087 = new GlBuffer(this.field_4100);
            this.method_3283(bufferBuilder_1, 16.0F, false);
            bufferBuilder_1.end();
            bufferBuilder_1.clear();
            this.field_4087.set(bufferBuilder_1.getByteBuffer());
        } else {
            this.field_4117 = GlAllocationUtils.genLists(1);
            GlStateManager.newList(this.field_4117, 4864);
            this.method_3283(bufferBuilder_1, 16.0F, false);
            tessellator_1.draw();
            GlStateManager.endList();
        }

    }

    private void method_3283(BufferBuilder bufferBuilder_1, float float_1, boolean boolean_1) {
        bufferBuilder_1.begin(7, VertexFormats.POSITION);

        for (int int_3 = -384; int_3 <= 384; int_3 += 64) {
            for (int int_4 = -384; int_4 <= 384; int_4 += 64) {
                float float_2 = (float) int_3;
                float float_3 = (float) (int_3 + 64);
                if (boolean_1) {
                    float_3 = (float) int_3;
                    float_2 = (float) (int_3 + 64);
                }

                bufferBuilder_1.vertex((double) float_2, (double) float_1, (double) int_4).next();
                bufferBuilder_1.vertex((double) float_3, (double) float_1, (double) int_4).next();
                bufferBuilder_1.vertex((double) float_3, (double) float_1, (double) (int_4 + 64)).next();
                bufferBuilder_1.vertex((double) float_2, (double) float_1, (double) (int_4 + 64)).next();
            }
        }

    }

    private void renderStars() {
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        if (this.starsBuffer != null) {
            this.starsBuffer.delete();
        }

        if (this.starsDisplayList >= 0) {
            GlAllocationUtils.deleteSingletonList(this.starsDisplayList);
            this.starsDisplayList = -1;
        }

        if (this.vertexBufferObjectsEnabled) {
            this.starsBuffer = new GlBuffer(this.field_4100);
            this.renderStars(bufferBuilder_1);
            bufferBuilder_1.end();
            bufferBuilder_1.clear();
            this.starsBuffer.set(bufferBuilder_1.getByteBuffer());
        } else {
            this.starsDisplayList = GlAllocationUtils.genLists(1);
            GlStateManager.pushMatrix();
            GlStateManager.newList(this.starsDisplayList, 4864);
            this.renderStars(bufferBuilder_1);
            tessellator_1.draw();
            GlStateManager.endList();
            GlStateManager.popMatrix();
        }

    }

    private void renderStars(BufferBuilder bufferBuilder_1) {
        Random random_1 = new Random(10842L);
        bufferBuilder_1.begin(7, VertexFormats.POSITION);

        for (int int_1 = 0; int_1 < 1500; ++int_1) {
            double double_1 = (double) (random_1.nextFloat() * 2.0F - 1.0F);
            double double_2 = (double) (random_1.nextFloat() * 2.0F - 1.0F);
            double double_3 = (double) (random_1.nextFloat() * 2.0F - 1.0F);
            double double_4 = (double) (0.15F + random_1.nextFloat() * 0.1F);
            double double_5 = double_1 * double_1 + double_2 * double_2 + double_3 * double_3;
            if (double_5 < 1.0D && double_5 > 0.01D) {
                double_5 = 1.0D / Math.sqrt(double_5);
                double_1 *= double_5;
                double_2 *= double_5;
                double_3 *= double_5;
                double double_6 = double_1 * 100.0D;
                double double_7 = double_2 * 100.0D;
                double double_8 = double_3 * 100.0D;
                double double_9 = Math.atan2(double_1, double_3);
                double double_10 = Math.sin(double_9);
                double double_11 = Math.cos(double_9);
                double double_12 = Math.atan2(Math.sqrt(double_1 * double_1 + double_3 * double_3), double_2);
                double double_13 = Math.sin(double_12);
                double double_14 = Math.cos(double_12);
                double double_15 = random_1.nextDouble() * 3.141592653589793D * 2.0D;
                double double_16 = Math.sin(double_15);
                double double_17 = Math.cos(double_15);

                for (int int_2 = 0; int_2 < 4; ++int_2) {
                    double double_18 = 0.0D;
                    double double_19 = (double) ((int_2 & 2) - 1) * double_4;
                    double double_20 = (double) ((int_2 + 1 & 2) - 1) * double_4;
                    double double_21 = 0.0D;
                    double double_22 = double_19 * double_17 - double_20 * double_16;
                    double double_23 = double_20 * double_17 + double_19 * double_16;
                    double double_25 = double_22 * double_13 + 0.0D * double_14;
                    double double_26 = 0.0D * double_13 - double_22 * double_14;
                    double double_27 = double_26 * double_10 - double_23 * double_11;
                    double double_29 = double_23 * double_10 + double_26 * double_11;
                    bufferBuilder_1.vertex(double_6 + double_27, double_7 + double_25, double_8 + double_29).next();
                }
            }
        }

    }

    public void setWorld(ClientWorld clientWorld_1) {
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        this.cameraChunkX = Integer.MIN_VALUE;
        this.cameraChunkY = Integer.MIN_VALUE;
        this.cameraChunkZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setWorld(clientWorld_1);
        this.world = clientWorld_1;
        if (clientWorld_1 != null) {
            this.reload();
        } else {
            this.chunkRenderers.clear();
            this.chunkInfos.clear();
            if (this.chunkRenderDispatcher != null) {
                this.chunkRenderDispatcher.delete();
                this.chunkRenderDispatcher = null;
            }

            if (this.chunkBatcher != null) {
                this.chunkBatcher.stop();
            }

            this.chunkBatcher = null;
            this.blockEntities.clear();
        }

    }

    public void reload() {
        if (this.world != null) {
            if (this.chunkBatcher == null) {
                this.chunkBatcher = new ChunkBatcher(this.client.is64Bit());
            }

            this.terrainUpdateNecessary = true;
            this.cloudsDirty = true;
            LeavesBlock.setRenderingMode(this.client.options.fancyGraphics);
            this.renderDistance = this.client.options.viewDistance;
            boolean boolean_1 = this.vertexBufferObjectsEnabled;
            this.vertexBufferObjectsEnabled = GLX.useVbo();
            if (boolean_1 && !this.vertexBufferObjectsEnabled) {
                this.chunkRendererList = new DisplayListChunkRendererList();
                this.chunkRendererFactory = uk.co.xsc.securitymod.DisplayListChunkRenderer::new;
            } else if (!boolean_1 && this.vertexBufferObjectsEnabled) {
                this.chunkRendererList = new uk.co.xsc.securitymod.VboChunkRendererList();
                this.chunkRendererFactory = uk.co.xsc.securitymod.ChunkRenderer::new;
            }

            if (boolean_1 != this.vertexBufferObjectsEnabled) {
                this.renderStars();
                this.method_3277();
                this.method_3265();
            }

            if (this.chunkRenderDispatcher != null) {
                this.chunkRenderDispatcher.delete();
            }

            this.clearChunkRenderers();
            synchronized (this.blockEntities) {
                this.blockEntities.clear();
            }

            this.chunkRenderDispatcher = new ChunkRenderDispatcher(this.world, this.client.options.viewDistance, this, this.chunkRendererFactory);
            if (this.world != null) {
                Entity entity_1 = this.client.getCameraEntity();
                if (entity_1 != null) {
                    this.chunkRenderDispatcher.updateCameraPosition(entity_1.x, entity_1.z);
                }
            }

            this.field_4076 = 2;
        }
    }

    protected void clearChunkRenderers() {
        this.chunkRenderers.clear();
        this.chunkBatcher.reset();
    }

    public void onResized(int int_1, int int_2) {
        this.scheduleTerrainUpdate();
        if (GLX.usePostProcess) {
            if (this.entityOutlineShader != null) {
                this.entityOutlineShader.setupDimensions(int_1, int_2);
            }

        }
    }

    public void renderEntities(Camera camera_1, VisibleRegion visibleRegion_1, float float_1) {
        if (this.field_4076 > 0) {
            --this.field_4076;
        } else {
            double double_1 = camera_1.getPos().x;
            double double_2 = camera_1.getPos().y;
            double double_3 = camera_1.getPos().z;
            this.world.getProfiler().push("prepare");
            BlockEntityRenderDispatcher.INSTANCE.configure(this.world, this.client.getTextureManager(), this.client.textRenderer, camera_1, this.client.hitResult);
            this.entityRenderDispatcher.configure(this.world, this.client.textRenderer, camera_1, this.client.targetedEntity, this.client.options);
            this.regularEntityCount = 0;
            this.blockEntityCount = 0;
            double double_4 = camera_1.getPos().x;
            double double_5 = camera_1.getPos().y;
            double double_6 = camera_1.getPos().z;
            BlockEntityRenderDispatcher.renderOffsetX = double_4;
            BlockEntityRenderDispatcher.renderOffsetY = double_5;
            BlockEntityRenderDispatcher.renderOffsetZ = double_6;
            this.entityRenderDispatcher.setRenderPosition(double_4, double_5, double_6);
            this.client.gameRenderer.enableLightmap();
            this.world.getProfiler().swap("entities");
            List<Entity> list_1 = Lists.newArrayList();
            List<Entity> list_2 = Lists.newArrayList();
            Iterator iterator_1 = this.world.getEntities().iterator();

            while (true) {
                Entity entity_1;
                do {
                    do {
                        if (!iterator_1.hasNext()) {
                            if (!list_2.isEmpty()) {
                                iterator_1 = list_2.iterator();

                                while (iterator_1.hasNext()) {
                                    entity_1 = (Entity) iterator_1.next();
                                    this.entityRenderDispatcher.renderSecondPass(entity_1, float_1);
                                }
                            }

                            if (this.canDrawEntityOutlines() && (!list_1.isEmpty() || this.entityOutlinesUpdateNecessary)) {
                                this.world.getProfiler().swap("entityOutlines");
                                this.entityOutlinesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                                this.entityOutlinesUpdateNecessary = !list_1.isEmpty();
                                if (!list_1.isEmpty()) {
                                    GlStateManager.depthFunc(519);
                                    GlStateManager.disableFog();
                                    this.entityOutlinesFramebuffer.beginWrite(false);
                                    GuiLighting.disable();
                                    this.entityRenderDispatcher.setRenderOutlines(true);

                                    for (int int_1 = 0; int_1 < list_1.size(); ++int_1) {
                                        this.entityRenderDispatcher.render((Entity) list_1.get(int_1), float_1, false);
                                    }

                                    this.entityRenderDispatcher.setRenderOutlines(false);
                                    GuiLighting.enable();
                                    GlStateManager.depthMask(false);
                                    this.entityOutlineShader.render(float_1);
                                    GlStateManager.enableLighting();
                                    GlStateManager.depthMask(true);
                                    GlStateManager.enableFog();
                                    GlStateManager.enableBlend();
                                    GlStateManager.enableColorMaterial();
                                    GlStateManager.depthFunc(515);
                                    GlStateManager.enableDepthTest();
                                    GlStateManager.enableAlphaTest();
                                }

                                this.frameBuffer.beginWrite(false);
                            }

                            this.world.getProfiler().swap("blockentities");
                            GuiLighting.enable();
                            iterator_1 = this.chunkInfos.iterator();

                            while (true) {
                                List list_3;
                                BlockEntity blockEntity_3;
                                do {
                                    if (!iterator_1.hasNext()) {
                                        synchronized (this.blockEntities) {
                                            Iterator var27 = this.blockEntities.iterator();

                                            while (true) {
                                                if (!var27.hasNext()) {
                                                    break;
                                                }

                                                BlockEntity blockEntity_2 = (BlockEntity) var27.next();
                                                BlockEntityRenderDispatcher.INSTANCE.render(blockEntity_2, float_1, -1);
                                            }
                                        }

                                        this.enableBlockOverlayRendering();
                                        iterator_1 = this.partiallyBrokenBlocks.values().iterator();

                                        while (iterator_1.hasNext()) {
                                            PartiallyBrokenBlockEntry partiallyBrokenBlockEntry_1 = (PartiallyBrokenBlockEntry) iterator_1.next();
                                            BlockPos blockPos_1 = partiallyBrokenBlockEntry_1.getPos();
                                            BlockState blockState_1 = this.world.getBlockState(blockPos_1);
                                            if (blockState_1.getBlock().hasBlockEntity()) {
                                                blockEntity_3 = this.world.getBlockEntity(blockPos_1);
                                                if (blockEntity_3 instanceof ChestBlockEntity && blockState_1.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) {
                                                    blockPos_1 = blockPos_1.offset(((Direction) blockState_1.get(ChestBlock.FACING)).rotateYClockwise());
                                                    blockEntity_3 = this.world.getBlockEntity(blockPos_1);
                                                }

                                                if (blockEntity_3 != null && blockState_1.hasBlockEntityBreakingRender()) {
                                                    BlockEntityRenderDispatcher.INSTANCE.render(blockEntity_3, float_1, partiallyBrokenBlockEntry_1.getStage());
                                                }
                                            }
                                        }

                                        this.disableBlockOverlayRendering();
                                        this.client.gameRenderer.disableLightmap();
                                        this.client.getProfiler().pop();
                                        return;
                                    }

                                    ChunkInfo worldRenderer$ChunkInfo_1 = (ChunkInfo) iterator_1.next();
                                    list_3 = worldRenderer$ChunkInfo_1.renderer.getData().getBlockEntities();
                                } while (list_3.isEmpty());

                                Iterator var21 = list_3.iterator();

                                while (var21.hasNext()) {
                                    blockEntity_3 = (BlockEntity) var21.next();
                                    BlockEntityRenderDispatcher.INSTANCE.render(blockEntity_3, float_1, -1);
                                }
                            }
                        }

                        entity_1 = (Entity) iterator_1.next();
                    } while (!this.entityRenderDispatcher.shouldRender(entity_1, visibleRegion_1, double_1, double_2, double_3) && !entity_1.hasPassengerDeep(this.client.player));
                } while (entity_1 == camera_1.getFocusedEntity() && !camera_1.isThirdPerson() && (!(camera_1.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity) camera_1.getFocusedEntity()).isSleeping()));

                ++this.regularEntityCount;
                this.entityRenderDispatcher.render(entity_1, float_1, false);
                if (entity_1.isGlowing() || entity_1 instanceof PlayerEntity && this.client.player.isSpectator() && this.client.options.keySpectatorOutlines.isPressed()) {
                    list_1.add(entity_1);
                }

                if (this.entityRenderDispatcher.hasSecondPass(entity_1)) {
                    list_2.add(entity_1);
                }
            }
        }
    }

    public String getChunksDebugString() {
        int int_1 = this.chunkRenderDispatcher.renderers.length;
        int int_2 = this.getChunkNumber();
        return String.format("C: %d/%d %sD: %d, %s", int_2, int_1, this.client.field_1730 ? "(s) " : "", this.renderDistance, this.chunkBatcher == null ? "null" : this.chunkBatcher.getDebugString());
    }

    protected int getChunkNumber() {
        int int_1 = 0;
        Iterator var2 = this.chunkInfos.iterator();

        while (var2.hasNext()) {
            ChunkInfo worldRenderer$ChunkInfo_1 = (ChunkInfo) var2.next();
            uk.co.xsc.securitymod.ChunkRenderData chunkRenderData_1 = worldRenderer$ChunkInfo_1.renderer.data;
            if (chunkRenderData_1 != uk.co.xsc.securitymod.ChunkRenderData.EMPTY && !chunkRenderData_1.isEmpty()) {
                ++int_1;
            }
        }

        return int_1;
    }

    public String getEntitiesDebugString() {
        return "E: " + this.regularEntityCount + "/" + this.world.getRegularEntityCount() + ", B: " + this.blockEntityCount;
    }

    public void setUpTerrain(Camera camera_1, VisibleRegion visibleRegion_1, int int_1, boolean boolean_1) {
        if (this.client.options.viewDistance != this.renderDistance) {
            this.reload();
        }

        this.world.getProfiler().push("camera");
        double double_1 = this.client.player.x - this.lastCameraChunkUpdateX;
        double double_2 = this.client.player.y - this.lastCameraChunkUpdateY;
        double double_3 = this.client.player.z - this.lastCameraChunkUpdateZ;
        if (this.cameraChunkX != this.client.player.chunkX || this.cameraChunkY != this.client.player.chunkY || this.cameraChunkZ != this.client.player.chunkZ || double_1 * double_1 + double_2 * double_2 + double_3 * double_3 > 16.0D) {
            this.lastCameraChunkUpdateX = this.client.player.x;
            this.lastCameraChunkUpdateY = this.client.player.y;
            this.lastCameraChunkUpdateZ = this.client.player.z;
            this.cameraChunkX = this.client.player.chunkX;
            this.cameraChunkY = this.client.player.chunkY;
            this.cameraChunkZ = this.client.player.chunkZ;
            this.chunkRenderDispatcher.updateCameraPosition(this.client.player.x, this.client.player.z);
        }

        this.world.getProfiler().swap("renderlistcamera");
        this.chunkRendererList.setCameraPosition(camera_1.getPos().x, camera_1.getPos().y, camera_1.getPos().z);
        this.chunkBatcher.setCameraPosition(camera_1.getPos());
        this.world.getProfiler().swap("cull");
        if (this.forcedFrustum != null) {
            FrustumWithOrigin frustumWithOrigin_1 = new FrustumWithOrigin(this.forcedFrustum);
            frustumWithOrigin_1.setOrigin(this.forcedFrustumPosition.x, this.forcedFrustumPosition.y, this.forcedFrustumPosition.z);
            visibleRegion_1 = frustumWithOrigin_1;
        }

        this.client.getProfiler().swap("culling");
        BlockPos blockPos_1 = camera_1.getBlockPos();
        uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1 = this.chunkRenderDispatcher.getChunkRenderer(blockPos_1);
        BlockPos blockPos_2 = new BlockPos(MathHelper.floor(camera_1.getPos().x / 16.0D) * 16, MathHelper.floor(camera_1.getPos().y / 16.0D) * 16, MathHelper.floor(camera_1.getPos().z / 16.0D) * 16);
        float float_1 = camera_1.getPitch();
        float float_2 = camera_1.getYaw();
        this.terrainUpdateNecessary = this.terrainUpdateNecessary || !this.chunkRenderers.isEmpty() || camera_1.getPos().x != this.lastCameraX || camera_1.getPos().y != this.lastCameraY || camera_1.getPos().z != this.lastCameraZ || (double) float_1 != this.lastCameraPitch || (double) float_2 != this.lastCameraYaw;
        this.lastCameraX = camera_1.getPos().x;
        this.lastCameraY = camera_1.getPos().y;
        this.lastCameraZ = camera_1.getPos().z;
        this.lastCameraPitch = (double) float_1;
        this.lastCameraYaw = (double) float_2;
        boolean boolean_2 = this.forcedFrustum != null;
        this.client.getProfiler().swap("update");
        ChunkInfo worldRenderer$ChunkInfo_2;
        uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_3;
        if (!boolean_2 && this.terrainUpdateNecessary) {
            this.terrainUpdateNecessary = false;
            this.chunkInfos = Lists.newArrayList();
            Queue<ChunkInfo> queue_1 = Queues.newArrayDeque();
            Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) this.client.options.viewDistance / 8.0D, 1.0D, 2.5D));
            boolean boolean_3 = this.client.field_1730;
            if (chunkRenderer_1 != null) {
                boolean boolean_4 = false;
                ChunkInfo worldRenderer$ChunkInfo_1 = new ChunkInfo(chunkRenderer_1, (Direction) null, 0);
                Set<Direction> set_1 = this.getOpenChunkFaces(blockPos_1);
                if (set_1.size() == 1) {
                    net.minecraft.util.math.Vec3d vec3d_1 = camera_1.method_19335();
                    Direction direction_1 = Direction.getFacing(vec3d_1.x, vec3d_1.y, vec3d_1.z).getOpposite();
                    set_1.remove(direction_1);
                }

                if (set_1.isEmpty()) {
                    boolean_4 = true;
                }

                if (boolean_4 && !boolean_1) {
                    this.chunkInfos.add(worldRenderer$ChunkInfo_1);
                } else {
                    if (boolean_1 && this.world.getBlockState(blockPos_1).isFullOpaque(this.world, blockPos_1)) {
                        boolean_3 = false;
                    }

                    chunkRenderer_1.method_3671(int_1);
                    queue_1.add(worldRenderer$ChunkInfo_1);
                }
            } else {
                int int_2 = blockPos_1.getY() > 0 ? 248 : 8;

                for (int int_3 = -this.renderDistance; int_3 <= this.renderDistance; ++int_3) {
                    for (int int_4 = -this.renderDistance; int_4 <= this.renderDistance; ++int_4) {
                        uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_2 = this.chunkRenderDispatcher.getChunkRenderer(new BlockPos((int_3 << 4) + 8, int_2, (int_4 << 4) + 8));
                        if (chunkRenderer_2 != null && ((VisibleRegion) visibleRegion_1).intersects(chunkRenderer_2.boundingBox)) {
                            chunkRenderer_2.method_3671(int_1);
                            queue_1.add(new ChunkInfo(chunkRenderer_2, (Direction) null, 0));
                        }
                    }
                }
            }

            this.client.getProfiler().push("iteration");

            while (!queue_1.isEmpty()) {
                worldRenderer$ChunkInfo_2 = (ChunkInfo) queue_1.poll();
                chunkRenderer_3 = worldRenderer$ChunkInfo_2.renderer;
                Direction direction_2 = worldRenderer$ChunkInfo_2.field_4125;
                this.chunkInfos.add(worldRenderer$ChunkInfo_2);
                Direction[] var39 = DIRECTIONS;
                int var41 = var39.length;

                for (int var24 = 0; var24 < var41; ++var24) {
                    Direction direction_3 = var39[var24];
                    uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_4 = this.getAdjacentChunkRenderer(blockPos_2, chunkRenderer_3, direction_3);
                    if ((!boolean_3 || !worldRenderer$ChunkInfo_2.method_3298(direction_3.getOpposite())) && (!boolean_3 || direction_2 == null || chunkRenderer_3.getData().isVisibleThrough(direction_2.getOpposite(), direction_3)) && chunkRenderer_4 != null && chunkRenderer_4.shouldBuild() && chunkRenderer_4.method_3671(int_1) && ((VisibleRegion) visibleRegion_1).intersects(chunkRenderer_4.boundingBox)) {
                        ChunkInfo worldRenderer$ChunkInfo_3 = new ChunkInfo(chunkRenderer_4, direction_3, worldRenderer$ChunkInfo_2.field_4122 + 1);
                        worldRenderer$ChunkInfo_3.method_3299(worldRenderer$ChunkInfo_2.field_4126, direction_3);
                        queue_1.add(worldRenderer$ChunkInfo_3);
                    }
                }
            }

            this.client.getProfiler().pop();
        }

        this.client.getProfiler().swap("captureFrustum");
        if (this.field_4066) {
            this.method_3275(camera_1.getPos().x, camera_1.getPos().y, camera_1.getPos().z);
            this.field_4066 = false;
        }

        this.client.getProfiler().swap("rebuildNear");
        Set<uk.co.xsc.securitymod.ChunkRenderer> set_2 = this.chunkRenderers;
        this.chunkRenderers = Sets.newLinkedHashSet();
        Iterator var30 = this.chunkInfos.iterator();

        while (true) {
            while (true) {
                do {
                    if (!var30.hasNext()) {
                        this.chunkRenderers.addAll(set_2);
                        this.client.getProfiler().pop();
                        return;
                    }

                    worldRenderer$ChunkInfo_2 = (ChunkInfo) var30.next();
                    chunkRenderer_3 = worldRenderer$ChunkInfo_2.renderer;
                } while (!chunkRenderer_3.shouldRebuild() && !set_2.contains(chunkRenderer_3));

                this.terrainUpdateNecessary = true;
                BlockPos blockPos_3 = chunkRenderer_3.getOrigin().add(8, 8, 8);
                boolean boolean_5 = blockPos_3.getSquaredDistance(blockPos_1) < 768.0D;
                if (!chunkRenderer_3.shouldRebuildOnClientThread() && !boolean_5) {
                    this.chunkRenderers.add(chunkRenderer_3);
                } else {
                    this.client.getProfiler().push("build near");
                    this.chunkBatcher.rebuildSync(chunkRenderer_3);
                    chunkRenderer_3.unscheduleRebuild();
                    this.client.getProfiler().pop();
                }
            }
        }
    }

    private Set<Direction> getOpenChunkFaces(BlockPos blockPos_1) {
        ChunkOcclusionGraphBuilder chunkOcclusionGraphBuilder_1 = new ChunkOcclusionGraphBuilder();
        BlockPos blockPos_2 = new BlockPos(blockPos_1.getX() >> 4 << 4, blockPos_1.getY() >> 4 << 4, blockPos_1.getZ() >> 4 << 4);
        WorldChunk worldChunk_1 = this.world.getWorldChunk(blockPos_2);
        Iterator var5 = BlockPos.iterate(blockPos_2, blockPos_2.add(15, 15, 15)).iterator();

        while (var5.hasNext()) {
            BlockPos blockPos_3 = (BlockPos) var5.next();
            if (worldChunk_1.getBlockState(blockPos_3).isFullOpaque(this.world, blockPos_3)) {
                chunkOcclusionGraphBuilder_1.markClosed(blockPos_3);
            }
        }

        return chunkOcclusionGraphBuilder_1.getOpenFaces(blockPos_1);
    }

    @Nullable
    private uk.co.xsc.securitymod.ChunkRenderer getAdjacentChunkRenderer(BlockPos blockPos_1, uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1, Direction direction_1) {
        BlockPos blockPos_2 = chunkRenderer_1.getNeighborPosition(direction_1);
        if (MathHelper.abs(blockPos_1.getX() - blockPos_2.getX()) > this.renderDistance * 16) {
            return null;
        } else if (blockPos_2.getY() >= 0 && blockPos_2.getY() < 256) {
            return MathHelper.abs(blockPos_1.getZ() - blockPos_2.getZ()) > this.renderDistance * 16 ? null : this.chunkRenderDispatcher.getChunkRenderer(blockPos_2);
        } else {
            return null;
        }
    }

    private void method_3275(double double_1, double double_2, double double_3) {
    }

    public int renderLayer(BlockRenderLayer blockRenderLayer_1, Camera camera_1) {
        GuiLighting.disable();
        if (blockRenderLayer_1 == BlockRenderLayer.TRANSLUCENT) {
            this.client.getProfiler().push("translucent_sort");
            double double_1 = camera_1.getPos().x - this.lastTranslucentSortX;
            double double_2 = camera_1.getPos().y - this.lastTranslucentSortY;
            double double_3 = camera_1.getPos().z - this.lastTranslucentSortZ;
            if (double_1 * double_1 + double_2 * double_2 + double_3 * double_3 > 1.0D) {
                this.lastTranslucentSortX = camera_1.getPos().x;
                this.lastTranslucentSortY = camera_1.getPos().y;
                this.lastTranslucentSortZ = camera_1.getPos().z;
                int int_1 = 0;
                Iterator var10 = this.chunkInfos.iterator();

                while (var10.hasNext()) {
                    ChunkInfo worldRenderer$ChunkInfo_1 = (ChunkInfo) var10.next();
                    if (worldRenderer$ChunkInfo_1.renderer.data.isBufferInitialized(blockRenderLayer_1) && int_1++ < 15) {
                        this.chunkBatcher.resortTransparency(worldRenderer$ChunkInfo_1.renderer);
                    }
                }
            }

            this.client.getProfiler().pop();
        }

        this.client.getProfiler().push("filterempty");
        int int_2 = 0;
        boolean boolean_1 = blockRenderLayer_1 == BlockRenderLayer.TRANSLUCENT;
        int int_3 = boolean_1 ? this.chunkInfos.size() - 1 : 0;
        int int_4 = boolean_1 ? -1 : this.chunkInfos.size();
        int int_5 = boolean_1 ? -1 : 1;

        for (int int_6 = int_3; int_6 != int_4; int_6 += int_5) {
            uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1 = ((ChunkInfo) this.chunkInfos.get(int_6)).renderer;
            if (!chunkRenderer_1.getData().isEmpty(blockRenderLayer_1)) {
                ++int_2;
                this.chunkRendererList.add(chunkRenderer_1, blockRenderLayer_1);
            }
        }

        this.client.getProfiler().swap(() -> {
            return "render_" + blockRenderLayer_1;
        });
        this.renderLayer(blockRenderLayer_1);
        this.client.getProfiler().pop();
        return int_2;
    }

    private void renderLayer(BlockRenderLayer blockRenderLayer_1) {
        this.client.gameRenderer.enableLightmap();
        if (GLX.useVbo()) {
            GlStateManager.enableClientState(32884);
            GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
            GlStateManager.enableClientState(32888);
            GLX.glClientActiveTexture(GLX.GL_TEXTURE1);
            GlStateManager.enableClientState(32888);
            GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
            GlStateManager.enableClientState(32886);
        }

        this.chunkRendererList.render(blockRenderLayer_1);
        if (GLX.useVbo()) {
            List<VertexFormatElement> list_1 = VertexFormats.POSITION_COLOR_UV_LMAP.getElements();
            Iterator var3 = list_1.iterator();

            while (var3.hasNext()) {
                VertexFormatElement vertexFormatElement_1 = (VertexFormatElement) var3.next();
                VertexFormatElement.Type vertexFormatElement$Type_1 = vertexFormatElement_1.getType();
                int int_1 = vertexFormatElement_1.getIndex();
                switch (vertexFormatElement$Type_1) {
                    case POSITION:
                        GlStateManager.disableClientState(32884);
                        break;
                    case UV:
                        GLX.glClientActiveTexture(GLX.GL_TEXTURE0 + int_1);
                        GlStateManager.disableClientState(32888);
                        GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
                        break;
                    case COLOR:
                        GlStateManager.disableClientState(32886);
                        GlStateManager.clearCurrentColor();
                }
            }
        }

        this.client.gameRenderer.disableLightmap();
    }

    private void removeOutdatedPartiallyBrokenBlocks(Iterator<PartiallyBrokenBlockEntry> iterator_1) {
        while (iterator_1.hasNext()) {
            PartiallyBrokenBlockEntry partiallyBrokenBlockEntry_1 = (PartiallyBrokenBlockEntry) iterator_1.next();
            int int_1 = partiallyBrokenBlockEntry_1.getLastUpdateTicks();
            if (this.ticks - int_1 > 400) {
                iterator_1.remove();
            }
        }

    }

    public void tick() {
        ++this.ticks;
        if (this.ticks % 20 == 0) {
            this.removeOutdatedPartiallyBrokenBlocks(this.partiallyBrokenBlocks.values().iterator());
        }

    }

    private void renderEndSky() {
        GlStateManager.disableFog();
        GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GuiLighting.disable();
        GlStateManager.depthMask(false);
        this.textureManager.bindTexture(END_SKY_TEX);
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();

        for (int int_1 = 0; int_1 < 6; ++int_1) {
            GlStateManager.pushMatrix();
            if (int_1 == 1) {
                GlStateManager.rotatef(90.0F, 1.0F, 0.0F, 0.0F);
            }

            if (int_1 == 2) {
                GlStateManager.rotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            }

            if (int_1 == 3) {
                GlStateManager.rotatef(180.0F, 1.0F, 0.0F, 0.0F);
            }

            if (int_1 == 4) {
                GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
            }

            if (int_1 == 5) {
                GlStateManager.rotatef(-90.0F, 0.0F, 0.0F, 1.0F);
            }

            bufferBuilder_1.begin(7, VertexFormats.POSITION_UV_COLOR);
            bufferBuilder_1.vertex(-100.0D, -100.0D, -100.0D).texture(0.0D, 0.0D).color(40, 40, 40, 255).next();
            bufferBuilder_1.vertex(-100.0D, -100.0D, 100.0D).texture(0.0D, 16.0D).color(40, 40, 40, 255).next();
            bufferBuilder_1.vertex(100.0D, -100.0D, 100.0D).texture(16.0D, 16.0D).color(40, 40, 40, 255).next();
            bufferBuilder_1.vertex(100.0D, -100.0D, -100.0D).texture(16.0D, 0.0D).color(40, 40, 40, 255).next();
            tessellator_1.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
    }

    public void renderSky(float float_1) {
        if (this.client.world.dimension.getType() == DimensionType.THE_END) {
            this.renderEndSky();
        } else if (this.client.world.dimension.hasVisibleSky()) {
            GlStateManager.disableTexture();
            net.minecraft.util.math.Vec3d vec3d_1 = this.world.getSkyColor(this.client.gameRenderer.getCamera().getBlockPos(), float_1);
            float float_2 = (float) vec3d_1.x;
            float float_3 = (float) vec3d_1.y;
            float float_4 = (float) vec3d_1.z;
            GlStateManager.color3f(float_2, float_3, float_4);
            Tessellator tessellator_1 = Tessellator.getInstance();
            BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
            GlStateManager.depthMask(false);
            GlStateManager.enableFog();
            GlStateManager.color3f(float_2, float_3, float_4);
            if (this.vertexBufferObjectsEnabled) {
                this.field_4087.bind();
                GlStateManager.enableClientState(32884);
                GlStateManager.vertexPointer(3, 5126, 12, 0);
                this.field_4087.draw(7);
                GlBuffer.unbind();
                GlStateManager.disableClientState(32884);
            } else {
                GlStateManager.callList(this.field_4117);
            }

            GlStateManager.disableFog();
            GlStateManager.disableAlphaTest();
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GuiLighting.disable();
            float[] floats_1 = this.world.dimension.getBackgroundColor(this.world.getSkyAngle(float_1), float_1);
            float float_11;
            float float_12;
            int int_2;
            float float_8;
            float float_9;
            float float_10;
            if (floats_1 != null) {
                GlStateManager.disableTexture();
                GlStateManager.shadeModel(7425);
                GlStateManager.pushMatrix();
                GlStateManager.rotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotatef(MathHelper.sin(this.world.getSkyAngleRadians(float_1)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
                float_11 = floats_1[0];
                float_12 = floats_1[1];
                float float_7 = floats_1[2];
                bufferBuilder_1.begin(6, VertexFormats.POSITION_COLOR);
                bufferBuilder_1.vertex(0.0D, 100.0D, 0.0D).color(float_11, float_12, float_7, floats_1[3]).next();

                for (int_2 = 0; int_2 <= 16; ++int_2) {
                    float_8 = (float) int_2 * 6.2831855F / 16.0F;
                    float_9 = MathHelper.sin(float_8);
                    float_10 = MathHelper.cos(float_8);
                    bufferBuilder_1.vertex((double) (float_9 * 120.0F), (double) (float_10 * 120.0F), (double) (-float_10 * 40.0F * floats_1[3])).color(floats_1[0], floats_1[1], floats_1[2], 0.0F).next();
                }

                tessellator_1.draw();
                GlStateManager.popMatrix();
                GlStateManager.shadeModel(7424);
            }

            GlStateManager.enableTexture();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();
            float_11 = 1.0F - this.world.getRainGradient(float_1);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, float_11);
            GlStateManager.rotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotatef(this.world.getSkyAngle(float_1) * 360.0F, 1.0F, 0.0F, 0.0F);
            float_12 = 30.0F;
            this.textureManager.bindTexture(SUN_TEX);
            bufferBuilder_1.begin(7, VertexFormats.POSITION_UV);
            bufferBuilder_1.vertex((double) (-float_12), 100.0D, (double) (-float_12)).texture(0.0D, 0.0D).next();
            bufferBuilder_1.vertex((double) float_12, 100.0D, (double) (-float_12)).texture(1.0D, 0.0D).next();
            bufferBuilder_1.vertex((double) float_12, 100.0D, (double) float_12).texture(1.0D, 1.0D).next();
            bufferBuilder_1.vertex((double) (-float_12), 100.0D, (double) float_12).texture(0.0D, 1.0D).next();
            tessellator_1.draw();
            float_12 = 20.0F;
            this.textureManager.bindTexture(MOON_PHASES_TEX);
            int int_3 = this.world.getMoonPhase();
            int int_4 = int_3 % 4;
            int_2 = int_3 / 4 % 2;
            float_8 = (float) (int_4 + 0) / 4.0F;
            float_9 = (float) (int_2 + 0) / 2.0F;
            float_10 = (float) (int_4 + 1) / 4.0F;
            float float_16 = (float) (int_2 + 1) / 2.0F;
            bufferBuilder_1.begin(7, VertexFormats.POSITION_UV);
            bufferBuilder_1.vertex((double) (-float_12), -100.0D, (double) float_12).texture((double) float_10, (double) float_16).next();
            bufferBuilder_1.vertex((double) float_12, -100.0D, (double) float_12).texture((double) float_8, (double) float_16).next();
            bufferBuilder_1.vertex((double) float_12, -100.0D, (double) (-float_12)).texture((double) float_8, (double) float_9).next();
            bufferBuilder_1.vertex((double) (-float_12), -100.0D, (double) (-float_12)).texture((double) float_10, (double) float_9).next();
            tessellator_1.draw();
            GlStateManager.disableTexture();
            float float_17 = this.world.getStarsBrightness(float_1) * float_11;
            if (float_17 > 0.0F) {
                GlStateManager.color4f(float_17, float_17, float_17, float_17);
                if (this.vertexBufferObjectsEnabled) {
                    this.starsBuffer.bind();
                    GlStateManager.enableClientState(32884);
                    GlStateManager.vertexPointer(3, 5126, 12, 0);
                    this.starsBuffer.draw(7);
                    GlBuffer.unbind();
                    GlStateManager.disableClientState(32884);
                } else {
                    GlStateManager.callList(this.starsDisplayList);
                }
            }

            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableAlphaTest();
            GlStateManager.enableFog();
            GlStateManager.popMatrix();
            GlStateManager.disableTexture();
            GlStateManager.color3f(0.0F, 0.0F, 0.0F);
            double double_1 = this.client.player.getCameraPosVec(float_1).y - this.world.getHorizonHeight();
            if (double_1 < 0.0D) {
                GlStateManager.pushMatrix();
                GlStateManager.translatef(0.0F, 12.0F, 0.0F);
                if (this.vertexBufferObjectsEnabled) {
                    this.field_4102.bind();
                    GlStateManager.enableClientState(32884);
                    GlStateManager.vertexPointer(3, 5126, 12, 0);
                    this.field_4102.draw(7);
                    GlBuffer.unbind();
                    GlStateManager.disableClientState(32884);
                } else {
                    GlStateManager.callList(this.field_4067);
                }

                GlStateManager.popMatrix();
            }

            if (this.world.dimension.method_12449()) {
                GlStateManager.color3f(float_2 * 0.2F + 0.04F, float_3 * 0.2F + 0.04F, float_4 * 0.6F + 0.1F);
            } else {
                GlStateManager.color3f(float_2, float_3, float_4);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translatef(0.0F, -((float) (double_1 - 16.0D)), 0.0F);
            GlStateManager.callList(this.field_4067);
            GlStateManager.popMatrix();
            GlStateManager.enableTexture();
            GlStateManager.depthMask(true);
        }
    }

    public void renderClouds(float float_1, double double_1, double double_2, double double_3) {
        if (this.client.world.dimension.hasVisibleSky()) {
            float float_2 = 12.0F;
            float float_3 = 4.0F;
            double double_4 = 2.0E-4D;
            double double_5 = (double) (((float) this.ticks + float_1) * 0.03F);
            double double_6 = (double_1 + double_5) / 12.0D;
            double double_7 = (double) (this.world.dimension.getCloudHeight() - (float) double_2 + 0.33F);
            double double_8 = double_3 / 12.0D + 0.33000001311302185D;
            double_6 -= (double) (MathHelper.floor(double_6 / 2048.0D) * 2048);
            double_8 -= (double) (MathHelper.floor(double_8 / 2048.0D) * 2048);
            float float_4 = (float) (double_6 - (double) MathHelper.floor(double_6));
            float float_5 = (float) (double_7 / 4.0D - (double) MathHelper.floor(double_7 / 4.0D)) * 4.0F;
            float float_6 = (float) (double_8 - (double) MathHelper.floor(double_8));
            net.minecraft.util.math.Vec3d vec3d_1 = this.world.getCloudColor(float_1);
            int int_1 = (int) Math.floor(double_6);
            int int_2 = (int) Math.floor(double_7 / 4.0D);
            int int_3 = (int) Math.floor(double_8);
            if (int_1 != this.field_4082 || int_2 != this.field_4097 || int_3 != this.field_4116 || this.client.options.getCloudRenderMode() != this.field_4080 || this.field_4072.squaredDistanceTo(vec3d_1) > 2.0E-4D) {
                this.field_4082 = int_1;
                this.field_4097 = int_2;
                this.field_4116 = int_3;
                this.field_4072 = vec3d_1;
                this.field_4080 = this.client.options.getCloudRenderMode();
                this.cloudsDirty = true;
            }

            if (this.cloudsDirty) {
                this.cloudsDirty = false;
                Tessellator tessellator_1 = Tessellator.getInstance();
                BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
                if (this.cloudsBuffer != null) {
                    this.cloudsBuffer.delete();
                }

                if (this.cloudsDisplayList >= 0) {
                    GlAllocationUtils.deleteSingletonList(this.cloudsDisplayList);
                    this.cloudsDisplayList = -1;
                }

                if (this.vertexBufferObjectsEnabled) {
                    this.cloudsBuffer = new GlBuffer(VertexFormats.POSITION_UV_COLOR_NORMAL);
                    this.renderClouds(bufferBuilder_1, double_6, double_7, double_8, vec3d_1);
                    bufferBuilder_1.end();
                    bufferBuilder_1.clear();
                    this.cloudsBuffer.set(bufferBuilder_1.getByteBuffer());
                } else {
                    this.cloudsDisplayList = GlAllocationUtils.genLists(1);
                    GlStateManager.newList(this.cloudsDisplayList, 4864);
                    this.renderClouds(bufferBuilder_1, double_6, double_7, double_8, vec3d_1);
                    tessellator_1.draw();
                    GlStateManager.endList();
                }
            }

            GlStateManager.disableCull();
            this.textureManager.bindTexture(CLOUDS_TEX);
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();
            GlStateManager.scalef(12.0F, 1.0F, 12.0F);
            GlStateManager.translatef(-float_4, float_5, -float_6);
            int int_6;
            int int_7;
            if (this.vertexBufferObjectsEnabled && this.cloudsBuffer != null) {
                this.cloudsBuffer.bind();
                GlStateManager.enableClientState(32884);
                GlStateManager.enableClientState(32888);
                GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
                GlStateManager.enableClientState(32886);
                GlStateManager.enableClientState(32885);
                GlStateManager.vertexPointer(3, 5126, 28, 0);
                GlStateManager.texCoordPointer(2, 5126, 28, 12);
                GlStateManager.colorPointer(4, 5121, 28, 20);
                GlStateManager.normalPointer(5120, 28, 24);
                int_6 = this.field_4080 == CloudRenderMode.FANCY ? 0 : 1;

                for (int_7 = int_6; int_7 < 2; ++int_7) {
                    if (int_7 == 0) {
                        GlStateManager.colorMask(false, false, false, false);
                    } else {
                        GlStateManager.colorMask(true, true, true, true);
                    }

                    this.cloudsBuffer.draw(7);
                }

                GlBuffer.unbind();
                GlStateManager.disableClientState(32884);
                GlStateManager.disableClientState(32888);
                GlStateManager.disableClientState(32886);
                GlStateManager.disableClientState(32885);
            } else if (this.cloudsDisplayList >= 0) {
                int_6 = this.field_4080 == CloudRenderMode.FANCY ? 0 : 1;

                for (int_7 = int_6; int_7 < 2; ++int_7) {
                    if (int_7 == 0) {
                        GlStateManager.colorMask(false, false, false, false);
                    } else {
                        GlStateManager.colorMask(true, true, true, true);
                    }

                    GlStateManager.callList(this.cloudsDisplayList);
                }
            }

            GlStateManager.popMatrix();
            GlStateManager.clearCurrentColor();
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
        }
    }

    private void renderClouds(BufferBuilder bufferBuilder_1, double double_1, double double_2, double double_3, net.minecraft.util.math.Vec3d vec3d_1) {
        float float_1 = 4.0F;
        float float_2 = 0.00390625F;
        float float_3 = 9.765625E-4F;
        float float_4 = (float) MathHelper.floor(double_1) * 0.00390625F;
        float float_5 = (float) MathHelper.floor(double_3) * 0.00390625F;
        float float_6 = (float) vec3d_1.x;
        float float_7 = (float) vec3d_1.y;
        float float_8 = (float) vec3d_1.z;
        float float_9 = float_6 * 0.9F;
        float float_10 = float_7 * 0.9F;
        float float_11 = float_8 * 0.9F;
        float float_12 = float_6 * 0.7F;
        float float_13 = float_7 * 0.7F;
        float float_14 = float_8 * 0.7F;
        float float_15 = float_6 * 0.8F;
        float float_16 = float_7 * 0.8F;
        float float_17 = float_8 * 0.8F;
        bufferBuilder_1.begin(7, VertexFormats.POSITION_UV_COLOR_NORMAL);
        float float_18 = (float) Math.floor(double_2 / 4.0D) * 4.0F;
        if (this.field_4080 == CloudRenderMode.FANCY) {
            for (int int_3 = -3; int_3 <= 4; ++int_3) {
                for (int int_4 = -3; int_4 <= 4; ++int_4) {
                    float float_19 = (float) (int_3 * 8);
                    float float_20 = (float) (int_4 * 8);
                    if (float_18 > -5.0F) {
                        bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 0.0F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_12, float_13, float_14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                        bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 0.0F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_12, float_13, float_14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                        bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 0.0F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_12, float_13, float_14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                        bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 0.0F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_12, float_13, float_14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                    }

                    if (float_18 <= 5.0F) {
                        bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 4.0F - 9.765625E-4F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
                        bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 4.0F - 9.765625E-4F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
                        bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 4.0F - 9.765625E-4F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
                        bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 4.0F - 9.765625E-4F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
                    }

                    int int_8;
                    if (int_3 > -1) {
                        for (int_8 = 0; int_8 < 8; ++int_8) {
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 0.0F), (double) (float_18 + 0.0F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 0.0F), (double) (float_18 + 4.0F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 0.0F), (double) (float_18 + 4.0F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 0.0F), (double) (float_18 + 0.0F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
                        }
                    }

                    if (int_3 <= 1) {
                        for (int_8 = 0; int_8 < 8; ++int_8) {
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 1.0F - 9.765625E-4F), (double) (float_18 + 0.0F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 1.0F - 9.765625E-4F), (double) (float_18 + 4.0F), (double) (float_20 + 8.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 8.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 1.0F - 9.765625E-4F), (double) (float_18 + 4.0F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + (float) int_8 + 1.0F - 9.765625E-4F), (double) (float_18 + 0.0F), (double) (float_20 + 0.0F)).texture((double) ((float_19 + (float) int_8 + 0.5F) * 0.00390625F + float_4), (double) ((float_20 + 0.0F) * 0.00390625F + float_5)).color(float_9, float_10, float_11, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
                        }
                    }

                    if (int_4 > -1) {
                        for (int_8 = 0; int_8 < 8; ++int_8) {
                            bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 4.0F), (double) (float_20 + (float) int_8 + 0.0F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 4.0F), (double) (float_20 + (float) int_8 + 0.0F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 0.0F), (double) (float_20 + (float) int_8 + 0.0F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 0.0F), (double) (float_20 + (float) int_8 + 0.0F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
                        }
                    }

                    if (int_4 <= 1) {
                        for (int_8 = 0; int_8 < 8; ++int_8) {
                            bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 4.0F), (double) (float_20 + (float) int_8 + 1.0F - 9.765625E-4F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 4.0F), (double) (float_20 + (float) int_8 + 1.0F - 9.765625E-4F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + 8.0F), (double) (float_18 + 0.0F), (double) (float_20 + (float) int_8 + 1.0F - 9.765625E-4F)).texture((double) ((float_19 + 8.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
                            bufferBuilder_1.vertex((double) (float_19 + 0.0F), (double) (float_18 + 0.0F), (double) (float_20 + (float) int_8 + 1.0F - 9.765625E-4F)).texture((double) ((float_19 + 0.0F) * 0.00390625F + float_4), (double) ((float_20 + (float) int_8 + 0.5F) * 0.00390625F + float_5)).color(float_15, float_16, float_17, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
                        }
                    }
                }
            }
        } else {

            for (int int_11 = -32; int_11 < 32; int_11 += 32) {
                for (int int_12 = -32; int_12 < 32; int_12 += 32) {
                    bufferBuilder_1.vertex((double) (int_11 + 0), (double) float_18, (double) (int_12 + 32)).texture((double) ((float) (int_11 + 0) * 0.00390625F + float_4), (double) ((float) (int_12 + 32) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                    bufferBuilder_1.vertex((double) (int_11 + 32), (double) float_18, (double) (int_12 + 32)).texture((double) ((float) (int_11 + 32) * 0.00390625F + float_4), (double) ((float) (int_12 + 32) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                    bufferBuilder_1.vertex((double) (int_11 + 32), (double) float_18, (double) (int_12 + 0)).texture((double) ((float) (int_11 + 32) * 0.00390625F + float_4), (double) ((float) (int_12 + 0) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                    bufferBuilder_1.vertex((double) (int_11 + 0), (double) float_18, (double) (int_12 + 0)).texture((double) ((float) (int_11 + 0) * 0.00390625F + float_4), (double) ((float) (int_12 + 0) * 0.00390625F + float_5)).color(float_6, float_7, float_8, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
                }
            }
        }

    }

    public void updateChunks(long long_1) {
        this.terrainUpdateNecessary |= this.chunkBatcher.runTasksSync(long_1);
        if (!this.chunkRenderers.isEmpty()) {
            Iterator iterator_1 = this.chunkRenderers.iterator();

            while (iterator_1.hasNext()) {
                uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1 = (uk.co.xsc.securitymod.ChunkRenderer) iterator_1.next();
                boolean boolean_2;
                if (chunkRenderer_1.shouldRebuildOnClientThread()) {
                    boolean_2 = this.chunkBatcher.rebuildSync(chunkRenderer_1);
                } else {
                    boolean_2 = this.chunkBatcher.rebuild(chunkRenderer_1);
                }

                if (!boolean_2) {
                    break;
                }

                chunkRenderer_1.unscheduleRebuild();
                iterator_1.remove();
                long long_2 = long_1 - SystemUtil.getMeasuringTimeNano();
                if (long_2 < 0L) {
                    break;
                }
            }
        }

    }

    public void renderWorldBorder(Camera camera_1, float float_1) {
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        WorldBorder worldBorder_1 = this.world.getWorldBorder();
        double double_1 = (double) (this.client.options.viewDistance * 16);
        if (camera_1.getPos().x >= worldBorder_1.getBoundEast() - double_1 || camera_1.getPos().x <= worldBorder_1.getBoundWest() + double_1 || camera_1.getPos().z >= worldBorder_1.getBoundSouth() - double_1 || camera_1.getPos().z <= worldBorder_1.getBoundNorth() + double_1) {
            double double_2 = 1.0D - worldBorder_1.contains(camera_1.getPos().x, camera_1.getPos().z) / double_1;
            double_2 = Math.pow(double_2, 4.0D);
            double double_3 = camera_1.getPos().x;
            double double_4 = camera_1.getPos().y;
            double double_5 = camera_1.getPos().z;
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            this.textureManager.bindTexture(FORCEFIELD_TEX);
            GlStateManager.depthMask(false);
            GlStateManager.pushMatrix();
            int int_1 = worldBorder_1.getStage().getColor();
            float float_2 = (float) (int_1 >> 16 & 255) / 255.0F;
            float float_3 = (float) (int_1 >> 8 & 255) / 255.0F;
            float float_4 = (float) (int_1 & 255) / 255.0F;
            GlStateManager.color4f(float_2, float_3, float_4, (float) double_2);
            GlStateManager.polygonOffset(-3.0F, -3.0F);
            GlStateManager.enablePolygonOffset();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableAlphaTest();
            GlStateManager.disableCull();
            float float_5 = (float) (SystemUtil.getMeasuringTimeMs() % 3000L) / 3000.0F;
            float float_6 = 0.0F;
            float float_7 = 0.0F;
            float float_8 = 128.0F;
            bufferBuilder_1.begin(7, VertexFormats.POSITION_UV);
            bufferBuilder_1.setOffset(-double_3, -double_4, -double_5);
            double double_6 = Math.max((double) MathHelper.floor(double_5 - double_1), worldBorder_1.getBoundNorth());
            double double_7 = Math.min((double) MathHelper.ceil(double_5 + double_1), worldBorder_1.getBoundSouth());
            float float_16;
            float float_15;
            double double_14;
            double double_15;
            if (double_3 > worldBorder_1.getBoundEast() - double_1) {
                float_15 = 0.0F;

                for (double_14 = double_6; double_14 < double_7; float_15 += 0.5F) {
                    double_15 = Math.min(1.0D, double_7 - double_14);
                    float_16 = (float) double_15 * 0.5F;
                    bufferBuilder_1.vertex(worldBorder_1.getBoundEast(), 256.0D, double_14).texture((double) (float_5 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(worldBorder_1.getBoundEast(), 256.0D, double_14 + double_15).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(worldBorder_1.getBoundEast(), 0.0D, double_14 + double_15).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 128.0F)).next();
                    bufferBuilder_1.vertex(worldBorder_1.getBoundEast(), 0.0D, double_14).texture((double) (float_5 + float_15), (double) (float_5 + 128.0F)).next();
                    ++double_14;
                }
            }

            if (double_3 < worldBorder_1.getBoundWest() + double_1) {
                float_15 = 0.0F;

                for (double_14 = double_6; double_14 < double_7; float_15 += 0.5F) {
                    double_15 = Math.min(1.0D, double_7 - double_14);
                    float_16 = (float) double_15 * 0.5F;
                    bufferBuilder_1.vertex(worldBorder_1.getBoundWest(), 256.0D, double_14).texture((double) (float_5 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(worldBorder_1.getBoundWest(), 256.0D, double_14 + double_15).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(worldBorder_1.getBoundWest(), 0.0D, double_14 + double_15).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 128.0F)).next();
                    bufferBuilder_1.vertex(worldBorder_1.getBoundWest(), 0.0D, double_14).texture((double) (float_5 + float_15), (double) (float_5 + 128.0F)).next();
                    ++double_14;
                }
            }

            double_6 = Math.max((double) MathHelper.floor(double_3 - double_1), worldBorder_1.getBoundWest());
            double_7 = Math.min((double) MathHelper.ceil(double_3 + double_1), worldBorder_1.getBoundEast());
            if (double_5 > worldBorder_1.getBoundSouth() - double_1) {
                float_15 = 0.0F;

                for (double_14 = double_6; double_14 < double_7; float_15 += 0.5F) {
                    double_15 = Math.min(1.0D, double_7 - double_14);
                    float_16 = (float) double_15 * 0.5F;
                    bufferBuilder_1.vertex(double_14, 256.0D, worldBorder_1.getBoundSouth()).texture((double) (float_5 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(double_14 + double_15, 256.0D, worldBorder_1.getBoundSouth()).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(double_14 + double_15, 0.0D, worldBorder_1.getBoundSouth()).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 128.0F)).next();
                    bufferBuilder_1.vertex(double_14, 0.0D, worldBorder_1.getBoundSouth()).texture((double) (float_5 + float_15), (double) (float_5 + 128.0F)).next();
                    ++double_14;
                }
            }

            if (double_5 < worldBorder_1.getBoundNorth() + double_1) {
                float_15 = 0.0F;

                for (double_14 = double_6; double_14 < double_7; float_15 += 0.5F) {
                    double_15 = Math.min(1.0D, double_7 - double_14);
                    float_16 = (float) double_15 * 0.5F;
                    bufferBuilder_1.vertex(double_14, 256.0D, worldBorder_1.getBoundNorth()).texture((double) (float_5 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(double_14 + double_15, 256.0D, worldBorder_1.getBoundNorth()).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 0.0F)).next();
                    bufferBuilder_1.vertex(double_14 + double_15, 0.0D, worldBorder_1.getBoundNorth()).texture((double) (float_5 + float_16 + float_15), (double) (float_5 + 128.0F)).next();
                    bufferBuilder_1.vertex(double_14, 0.0D, worldBorder_1.getBoundNorth()).texture((double) (float_5 + float_15), (double) (float_5 + 128.0F)).next();
                    ++double_14;
                }
            }

            tessellator_1.draw();
            bufferBuilder_1.setOffset(0.0D, 0.0D, 0.0D);
            GlStateManager.enableCull();
            GlStateManager.disableAlphaTest();
            GlStateManager.polygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
            GlStateManager.enableAlphaTest();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
        }
    }

    private void enableBlockOverlayRendering() {
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableBlend();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 0.5F);
        GlStateManager.polygonOffset(-1.0F, -10.0F);
        GlStateManager.enablePolygonOffset();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableAlphaTest();
        GlStateManager.pushMatrix();
    }

    private void disableBlockOverlayRendering() {
        GlStateManager.disableAlphaTest();
        GlStateManager.polygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableAlphaTest();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }

    public void renderPartiallyBrokenBlocks(Tessellator tessellator_1, BufferBuilder bufferBuilder_1, Camera camera_1) {
        double double_1 = camera_1.getPos().x;
        double double_2 = camera_1.getPos().y;
        double double_3 = camera_1.getPos().z;
        if (!this.partiallyBrokenBlocks.isEmpty()) {
            this.textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            this.enableBlockOverlayRendering();
            bufferBuilder_1.begin(7, VertexFormats.POSITION_COLOR_UV_LMAP);
            bufferBuilder_1.setOffset(-double_1, -double_2, -double_3);
            bufferBuilder_1.disableColor();
            Iterator iterator_1 = this.partiallyBrokenBlocks.values().iterator();

            while (iterator_1.hasNext()) {
                PartiallyBrokenBlockEntry partiallyBrokenBlockEntry_1 = (PartiallyBrokenBlockEntry) iterator_1.next();
                BlockPos blockPos_1 = partiallyBrokenBlockEntry_1.getPos();
                Block block_1 = this.world.getBlockState(blockPos_1).getBlock();
                if (!(block_1 instanceof ChestBlock) && !(block_1 instanceof EnderChestBlock) && !(block_1 instanceof AbstractSignBlock) && !(block_1 instanceof AbstractSkullBlock)) {
                    double double_4 = (double) blockPos_1.getX() - double_1;
                    double double_5 = (double) blockPos_1.getY() - double_2;
                    double double_6 = (double) blockPos_1.getZ() - double_3;
                    if (double_4 * double_4 + double_5 * double_5 + double_6 * double_6 > 1024.0D) {
                        iterator_1.remove();
                    } else {
                        BlockState blockState_1 = this.world.getBlockState(blockPos_1);
                        if (!blockState_1.isAir()) {
                            int int_1 = partiallyBrokenBlockEntry_1.getStage();
                            Sprite sprite_1 = this.destroyStages[int_1];
                            BlockRenderManager blockRenderManager_1 = this.client.getBlockRenderManager();
                            blockRenderManager_1.tesselateDamage(blockState_1, blockPos_1, sprite_1, this.world);
                        }
                    }
                }
            }

            tessellator_1.draw();
            bufferBuilder_1.setOffset(0.0D, 0.0D, 0.0D);
            this.disableBlockOverlayRendering();
        }

    }

    public void drawHighlightedBlockOutline(Camera camera_1, HitResult hitResult_1, int int_1) {
        if (int_1 == 0 && hitResult_1.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos_1 = ((BlockHitResult) hitResult_1).getBlockPos();
            BlockState blockState_1 = this.world.getBlockState(blockPos_1);
            if (!blockState_1.isAir() && this.world.getWorldBorder().contains(blockPos_1)) {
                GlStateManager.enableBlend();
                GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.lineWidth(Math.max(2.5F, (float) this.client.window.getFramebufferWidth() / 1920.0F * 2.5F));
                GlStateManager.disableTexture();
                GlStateManager.depthMask(false);
                GlStateManager.matrixMode(5889);
                GlStateManager.pushMatrix();
                GlStateManager.scalef(1.0F, 1.0F, 0.999F);
                double double_1 = camera_1.getPos().x;
                double double_2 = camera_1.getPos().y;
                double double_3 = camera_1.getPos().z;
                drawShapeOutline(blockState_1.getOutlineShape(this.world, blockPos_1, EntityContext.of(camera_1.getFocusedEntity())), (double) blockPos_1.getX() - double_1, (double) blockPos_1.getY() - double_2, (double) blockPos_1.getZ() - double_3, 0.0F, 0.0F, 0.0F, 0.4F);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(5888);
                GlStateManager.depthMask(true);
                GlStateManager.enableTexture();
                GlStateManager.disableBlend();
            }
        }

    }

    public void updateBlock(BlockView blockView_1, BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2, int int_1) {
        this.scheduleSectionRender(blockPos_1, (int_1 & 8) != 0);
    }

    private void scheduleSectionRender(BlockPos blockPos_1, boolean boolean_1) {
        for (int int_1 = blockPos_1.getZ() - 1; int_1 <= blockPos_1.getZ() + 1; ++int_1) {
            for (int int_2 = blockPos_1.getX() - 1; int_2 <= blockPos_1.getX() + 1; ++int_2) {
                for (int int_3 = blockPos_1.getY() - 1; int_3 <= blockPos_1.getY() + 1; ++int_3) {
                    this.scheduleChunkRender(int_2 >> 4, int_3 >> 4, int_1 >> 4, boolean_1);
                }
            }
        }

    }

    public void scheduleBlockRenders(int int_1, int int_2, int int_3, int int_4, int int_5, int int_6) {
        for (int int_7 = int_3 - 1; int_7 <= int_6 + 1; ++int_7) {
            for (int int_8 = int_1 - 1; int_8 <= int_4 + 1; ++int_8) {
                for (int int_9 = int_2 - 1; int_9 <= int_5 + 1; ++int_9) {
                    this.scheduleBlockRender(int_8 >> 4, int_9 >> 4, int_7 >> 4);
                }
            }
        }

    }

    public void scheduleBlockRenders(int int_1, int int_2, int int_3) {
        for (int int_4 = int_3 - 1; int_4 <= int_3 + 1; ++int_4) {
            for (int int_5 = int_1 - 1; int_5 <= int_1 + 1; ++int_5) {
                for (int int_6 = int_2 - 1; int_6 <= int_2 + 1; ++int_6) {
                    this.scheduleBlockRender(int_5, int_6, int_4);
                }
            }
        }

    }

    public void scheduleBlockRender(int int_1, int int_2, int int_3) {
        this.scheduleChunkRender(int_1, int_2, int_3, false);
    }

    private void scheduleChunkRender(int int_1, int int_2, int int_3, boolean boolean_1) {
        this.chunkRenderDispatcher.scheduleChunkRender(int_1, int_2, int_3, boolean_1);
    }

    public void playSong(@Nullable SoundEvent soundEvent_1, BlockPos blockPos_1) {
        SoundInstance soundInstance_1 = (SoundInstance) this.playingSongs.get(blockPos_1);
        if (soundInstance_1 != null) {
            this.client.getSoundManager().stop(soundInstance_1);
            this.playingSongs.remove(blockPos_1);
        }

        if (soundEvent_1 != null) {
            MusicDiscItem musicDiscItem_1 = MusicDiscItem.bySound(soundEvent_1);
            if (musicDiscItem_1 != null) {
                this.client.inGameHud.setRecordPlayingOverlay(musicDiscItem_1.getDescription().getFormattedText());
            }

            soundInstance_1 = PositionedSoundInstance.record(soundEvent_1, (float) blockPos_1.getX(), (float) blockPos_1.getY(), (float) blockPos_1.getZ());
            this.playingSongs.put(blockPos_1, soundInstance_1);
            this.client.getSoundManager().play(soundInstance_1);
        }

        this.updateEntitiesForSong(this.world, blockPos_1, soundEvent_1 != null);
    }

    private void updateEntitiesForSong(World world_1, BlockPos blockPos_1, boolean boolean_1) {
        List<LivingEntity> list_1 = world_1.getEntities(LivingEntity.class, (new BoundingBox(blockPos_1)).expand(3.0D));
        Iterator var5 = list_1.iterator();

        while (var5.hasNext()) {
            LivingEntity livingEntity_1 = (LivingEntity) var5.next();
            livingEntity_1.setNearbySongPlaying(blockPos_1, boolean_1);
        }

    }

    public void addParticle(ParticleEffect particleEffect_1, boolean boolean_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        this.addParticle(particleEffect_1, boolean_1, false, double_1, double_2, double_3, double_4, double_5, double_6);
    }

    public void addParticle(ParticleEffect particleEffect_1, boolean boolean_1, boolean boolean_2, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        try {
            this.spawnParticle(particleEffect_1, boolean_1, boolean_2, double_1, double_2, double_3, double_4, double_5, double_6);
        } catch (Throwable var19) {
            CrashReport crashReport_1 = CrashReport.create(var19, "Exception while adding particle");
            CrashReportSection crashReportSection_1 = crashReport_1.addElement("Particle being added");
            crashReportSection_1.add("ID", (Object) Registry.PARTICLE_TYPE.getId(particleEffect_1.getType()));
            crashReportSection_1.add("Parameters", (Object) particleEffect_1.asString());
            crashReportSection_1.add("Position", () -> {
                return CrashReportSection.createPositionString(double_1, double_2, double_3);
            });
            throw new CrashException(crashReport_1);
        }
    }

    private <T extends ParticleEffect> void addParticle(T particleEffect_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        this.addParticle(particleEffect_1, particleEffect_1.getType().shouldAlwaysSpawn(), double_1, double_2, double_3, double_4, double_5, double_6);
    }

    @Nullable
    private Particle spawnParticle(ParticleEffect particleEffect_1, boolean boolean_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        return this.spawnParticle(particleEffect_1, boolean_1, false, double_1, double_2, double_3, double_4, double_5, double_6);
    }

    @Nullable
    private Particle spawnParticle(ParticleEffect particleEffect_1, boolean boolean_1, boolean boolean_2, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        Camera camera_1 = this.client.gameRenderer.getCamera();
        if (this.client != null && camera_1.isReady() && this.client.particleManager != null) {
            ParticlesOption particlesOption_1 = this.getRandomParticleSpawnChance(boolean_2);
            if (boolean_1) {
                return this.client.particleManager.addParticle(particleEffect_1, double_1, double_2, double_3, double_4, double_5, double_6);
            } else if (camera_1.getPos().squaredDistanceTo(double_1, double_2, double_3) > 1024.0D) {
                return null;
            } else {
                return particlesOption_1 == ParticlesOption.MINIMAL ? null : this.client.particleManager.addParticle(particleEffect_1, double_1, double_2, double_3, double_4, double_5, double_6);
            }
        } else {
            return null;
        }
    }

    private ParticlesOption getRandomParticleSpawnChance(boolean boolean_1) {
        ParticlesOption particlesOption_1 = this.client.options.particles;
        if (boolean_1 && particlesOption_1 == ParticlesOption.MINIMAL && this.world.random.nextInt(10) == 0) {
            particlesOption_1 = ParticlesOption.DECREASED;
        }

        if (particlesOption_1 == ParticlesOption.DECREASED && this.world.random.nextInt(3) == 0) {
            particlesOption_1 = ParticlesOption.MINIMAL;
        }

        return particlesOption_1;
    }

    public void method_3267() {
    }

    public void playGlobalEvent(int int_1, BlockPos blockPos_1, int int_2) {
        switch (int_1) {
            case 1023:
            case 1028:
            case 1038:
                Camera camera_1 = this.client.gameRenderer.getCamera();
                if (camera_1.isReady()) {
                    double double_1 = (double) blockPos_1.getX() - camera_1.getPos().x;
                    double double_2 = (double) blockPos_1.getY() - camera_1.getPos().y;
                    double double_3 = (double) blockPos_1.getZ() - camera_1.getPos().z;
                    double double_4 = Math.sqrt(double_1 * double_1 + double_2 * double_2 + double_3 * double_3);
                    double double_5 = camera_1.getPos().x;
                    double double_6 = camera_1.getPos().y;
                    double double_7 = camera_1.getPos().z;
                    if (double_4 > 0.0D) {
                        double_5 += double_1 / double_4 * 2.0D;
                        double_6 += double_2 / double_4 * 2.0D;
                        double_7 += double_3 / double_4 * 2.0D;
                    }

                    if (int_1 == 1023) {
                        this.world.playSound(double_5, double_6, double_7, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.0F, 1.0F, false);
                    } else if (int_1 == 1038) {
                        this.world.playSound(double_5, double_6, double_7, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0F, 1.0F, false);
                    } else {
                        this.world.playSound(double_5, double_6, double_7, SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 5.0F, 1.0F, false);
                    }
                }
            default:
        }
    }

    public void playLevelEvent(PlayerEntity playerEntity_1, int int_1, BlockPos blockPos_1, int int_2) {
        Random random_1 = this.world.random;
        double double_15;
        int int_3;
        double double_17;
        double double_14;
        double double_3;
        double double_18;
        double double_19;
        double double_20;
        double double_21;
        double double_22;
        double double_16;
        int int_7;
        int int_9;
        switch (int_1) {
            case 1000:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1001:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1002:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1003:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.NEUTRAL, 1.0F, 1.2F, false);
                break;
            case 1004:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.2F, false);
                break;
            case 1005:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1006:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1007:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1008:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_FENCE_GATE_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1009:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random_1.nextFloat() - random_1.nextFloat()) * 0.8F, false);
                break;
            case 1010:
                if (Item.byRawId(int_2) instanceof MusicDiscItem) {
                    this.playSong(((MusicDiscItem) Item.byRawId(int_2)).getSound(), blockPos_1);
                } else {
                    this.playSong((SoundEvent) null, blockPos_1);
                }
                break;
            case 1011:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1012:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1013:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1014:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_FENCE_GATE_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1015:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_GHAST_WARN, SoundCategory.HOSTILE, 10.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1016:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 10.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1017:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.HOSTILE, 10.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1018:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1019:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1020:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1021:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1022:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1024:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1025:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_BAT_TAKEOFF, SoundCategory.NEUTRAL, 0.05F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1026:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1027:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.NEUTRAL, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1029:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1030:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1031:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.3F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1032:
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_PORTAL_TRAVEL, random_1.nextFloat() * 0.4F + 0.8F));
                break;
            case 1033:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1034:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1035:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1036:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1037:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1039:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_PHANTOM_BITE, SoundCategory.HOSTILE, 0.3F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1040:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, SoundCategory.NEUTRAL, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1041:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, SoundCategory.NEUTRAL, 2.0F, (random_1.nextFloat() - random_1.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1042:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1043:
                this.world.playSound(blockPos_1, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1500:
                ComposterBlock.playEffects(this.world, blockPos_1, int_2 > 0);
                break;
            case 1501:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (this.world.getRandom().nextFloat() - this.world.getRandom().nextFloat()) * 0.8F, false);

                for (int_3 = 0; int_3 < 8; ++int_3) {
                    this.world.addParticle(ParticleTypes.LARGE_SMOKE, (double) blockPos_1.getX() + Math.random(), (double) blockPos_1.getY() + 1.2D, (double) blockPos_1.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
                }

                return;
            case 1502:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.BLOCKS, 0.5F, 2.6F + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.8F, false);

                for (int_3 = 0; int_3 < 5; ++int_3) {
                    double_16 = (double) blockPos_1.getX() + random_1.nextDouble() * 0.6D + 0.2D;
                    double_17 = (double) blockPos_1.getY() + random_1.nextDouble() * 0.6D + 0.2D;
                    double_14 = (double) blockPos_1.getZ() + random_1.nextDouble() * 0.6D + 0.2D;
                    this.world.addParticle(ParticleTypes.SMOKE, double_16, double_17, double_14, 0.0D, 0.0D, 0.0D);
                }

                return;
            case 1503:
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);

                for (int_3 = 0; int_3 < 16; ++int_3) {
                    double_16 = (double) ((float) blockPos_1.getX() + (5.0F + random_1.nextFloat() * 6.0F) / 16.0F);
                    double_17 = (double) ((float) blockPos_1.getY() + 0.8125F);
                    double_14 = (double) ((float) blockPos_1.getZ() + (5.0F + random_1.nextFloat() * 6.0F) / 16.0F);
                    double_3 = 0.0D;
                    double double_33 = 0.0D;
                    double double_34 = 0.0D;
                    this.world.addParticle(ParticleTypes.SMOKE, double_16, double_17, double_14, 0.0D, 0.0D, 0.0D);
                }

                return;
            case 2000:
                Direction direction_1 = Direction.byId(int_2);
                int_3 = direction_1.getOffsetX();
                int int_4 = direction_1.getOffsetY();
                int int_5 = direction_1.getOffsetZ();
                double_17 = (double) blockPos_1.getX() + (double) int_3 * 0.6D + 0.5D;
                double_14 = (double) blockPos_1.getY() + (double) int_4 * 0.6D + 0.5D;
                double_3 = (double) blockPos_1.getZ() + (double) int_5 * 0.6D + 0.5D;

                for (int_9 = 0; int_9 < 10; ++int_9) {
                    double_18 = random_1.nextDouble() * 0.2D + 0.01D;
                    double_19 = double_17 + (double) int_3 * 0.01D + (random_1.nextDouble() - 0.5D) * (double) int_5 * 0.5D;
                    double_20 = double_14 + (double) int_4 * 0.01D + (random_1.nextDouble() - 0.5D) * (double) int_4 * 0.5D;
                    double_21 = double_3 + (double) int_5 * 0.01D + (random_1.nextDouble() - 0.5D) * (double) int_3 * 0.5D;
                    double_22 = (double) int_3 * double_18 + random_1.nextGaussian() * 0.01D;
                    double double_9 = (double) int_4 * double_18 + random_1.nextGaussian() * 0.01D;
                    double double_10 = (double) int_5 * double_18 + random_1.nextGaussian() * 0.01D;
                    this.addParticle(ParticleTypes.SMOKE, double_19, double_20, double_21, double_22, double_9, double_10);
                }

                return;
            case 2001:
                BlockState blockState_1 = Block.getStateFromRawId(int_2);
                if (!blockState_1.isAir()) {
                    BlockSoundGroup blockSoundGroup_1 = blockState_1.getSoundGroup();
                    this.world.playSound(blockPos_1, blockSoundGroup_1.getBreakSound(), SoundCategory.BLOCKS, (blockSoundGroup_1.getVolume() + 1.0F) / 2.0F, blockSoundGroup_1.getPitch() * 0.8F, false);
                }

                this.client.particleManager.addBlockBreakParticles(blockPos_1, blockState_1);
                break;
            case 2002:
            case 2007:
                double_15 = (double) blockPos_1.getX();
                double_16 = (double) blockPos_1.getY();
                double_17 = (double) blockPos_1.getZ();

                for (int_7 = 0; int_7 < 8; ++int_7) {
                    this.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)), double_15, double_16, double_17, random_1.nextGaussian() * 0.15D, random_1.nextDouble() * 0.2D, random_1.nextGaussian() * 0.15D);
                }

                float float_1 = (float) (int_2 >> 16 & 255) / 255.0F;
                float float_2 = (float) (int_2 >> 8 & 255) / 255.0F;
                float float_3 = (float) (int_2 >> 0 & 255) / 255.0F;
                ParticleEffect particleEffect_1 = int_1 == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

                for (int_9 = 0; int_9 < 100; ++int_9) {
                    double_18 = random_1.nextDouble() * 4.0D;
                    double_19 = random_1.nextDouble() * 3.141592653589793D * 2.0D;
                    double_20 = Math.cos(double_19) * double_18;
                    double_21 = 0.01D + random_1.nextDouble() * 0.5D;
                    double_22 = Math.sin(double_19) * double_18;
                    Particle particle_1 = this.spawnParticle(particleEffect_1, particleEffect_1.getType().shouldAlwaysSpawn(), double_15 + double_20 * 0.1D, double_16 + 0.3D, double_17 + double_22 * 0.1D, double_20, double_21, double_22);
                    if (particle_1 != null) {
                        float float_4 = 0.75F + random_1.nextFloat() * 0.25F;
                        particle_1.setColor(float_1 * float_4, float_2 * float_4, float_3 * float_4);
                        particle_1.move((float) double_18);
                    }
                }

                this.world.playSound(blockPos_1, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.NEUTRAL, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2003:
                double_15 = (double) blockPos_1.getX() + 0.5D;
                double_16 = (double) blockPos_1.getY();
                double_17 = (double) blockPos_1.getZ() + 0.5D;

                for (int_7 = 0; int_7 < 8; ++int_7) {
                    this.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)), double_15, double_16, double_17, random_1.nextGaussian() * 0.15D, random_1.nextDouble() * 0.2D, random_1.nextGaussian() * 0.15D);
                }

                for (double_14 = 0.0D; double_14 < 6.283185307179586D; double_14 += 0.15707963267948966D) {
                    this.addParticle(ParticleTypes.PORTAL, double_15 + Math.cos(double_14) * 5.0D, double_16 - 0.4D, double_17 + Math.sin(double_14) * 5.0D, Math.cos(double_14) * -5.0D, 0.0D, Math.sin(double_14) * -5.0D);
                    this.addParticle(ParticleTypes.PORTAL, double_15 + Math.cos(double_14) * 5.0D, double_16 - 0.4D, double_17 + Math.sin(double_14) * 5.0D, Math.cos(double_14) * -7.0D, 0.0D, Math.sin(double_14) * -7.0D);
                }

                return;
            case 2004:
                for (int_3 = 0; int_3 < 20; ++int_3) {
                    double_16 = (double) blockPos_1.getX() + 0.5D + ((double) this.world.random.nextFloat() - 0.5D) * 2.0D;
                    double_17 = (double) blockPos_1.getY() + 0.5D + ((double) this.world.random.nextFloat() - 0.5D) * 2.0D;
                    double_14 = (double) blockPos_1.getZ() + 0.5D + ((double) this.world.random.nextFloat() - 0.5D) * 2.0D;
                    this.world.addParticle(ParticleTypes.SMOKE, double_16, double_17, double_14, 0.0D, 0.0D, 0.0D);
                    this.world.addParticle(ParticleTypes.FLAME, double_16, double_17, double_14, 0.0D, 0.0D, 0.0D);
                }

                return;
            case 2005:
                BoneMealItem.playEffects(this.world, blockPos_1, int_2);
                break;
            case 2006:
                for (int_3 = 0; int_3 < 200; ++int_3) {
                    float float_5 = random_1.nextFloat() * 4.0F;
                    float float_6 = random_1.nextFloat() * 6.2831855F;
                    double_17 = (double) (MathHelper.cos(float_6) * float_5);
                    double_14 = 0.01D + random_1.nextDouble() * 0.5D;
                    double_3 = (double) (MathHelper.sin(float_6) * float_5);
                    Particle particle_2 = this.spawnParticle(ParticleTypes.DRAGON_BREATH, false, (double) blockPos_1.getX() + double_17 * 0.1D, (double) blockPos_1.getY() + 0.3D, (double) blockPos_1.getZ() + double_3 * 0.1D, double_17, double_14, double_3);
                    if (particle_2 != null) {
                        particle_2.move(float_5);
                    }
                }

                this.world.playSound(blockPos_1, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2008:
                this.world.addParticle(ParticleTypes.EXPLOSION, (double) blockPos_1.getX() + 0.5D, (double) blockPos_1.getY() + 0.5D, (double) blockPos_1.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
                break;
            case 3000:
                this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, (double) blockPos_1.getX() + 0.5D, (double) blockPos_1.getY() + 0.5D, (double) blockPos_1.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
                this.world.playSound(blockPos_1, SoundEvents.BLOCK_END_GATEWAY_SPAWN, SoundCategory.BLOCKS, 10.0F, (1.0F + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2F) * 0.7F, false);
                break;
            case 3001:
                this.world.playSound(blockPos_1, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 64.0F, 0.8F + this.world.random.nextFloat() * 0.3F, false);
        }

    }

    public void setBlockBreakingProgress(int int_1, BlockPos blockPos_1, int int_2) {
        if (int_2 >= 0 && int_2 < 10) {
            PartiallyBrokenBlockEntry partiallyBrokenBlockEntry_1 = (PartiallyBrokenBlockEntry) this.partiallyBrokenBlocks.get(int_1);
            if (partiallyBrokenBlockEntry_1 == null || partiallyBrokenBlockEntry_1.getPos().getX() != blockPos_1.getX() || partiallyBrokenBlockEntry_1.getPos().getY() != blockPos_1.getY() || partiallyBrokenBlockEntry_1.getPos().getZ() != blockPos_1.getZ()) {
                partiallyBrokenBlockEntry_1 = new PartiallyBrokenBlockEntry(int_1, blockPos_1);
                this.partiallyBrokenBlocks.put(int_1, partiallyBrokenBlockEntry_1);
            }

            partiallyBrokenBlockEntry_1.setStage(int_2);
            partiallyBrokenBlockEntry_1.setLastUpdateTicks(this.ticks);
        } else {
            this.partiallyBrokenBlocks.remove(int_1);
        }

    }

    public boolean isTerrainRenderComplete() {
        return this.chunkRenderers.isEmpty() && this.chunkBatcher.isEmpty();
    }

    public void scheduleTerrainUpdate() {
        this.terrainUpdateNecessary = true;
        this.cloudsDirty = true;
    }

    public void updateBlockEntities(Collection<BlockEntity> collection_1, Collection<BlockEntity> collection_2) {
        synchronized (this.blockEntities) {
            this.blockEntities.removeAll(collection_1);
            this.blockEntities.addAll(collection_2);
        }
    }

    @Environment(EnvType.CLIENT)
    class ChunkInfo {
        private final uk.co.xsc.securitymod.ChunkRenderer renderer;
        private final Direction field_4125;
        private final int field_4122;
        private byte field_4126;

        private ChunkInfo(uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1, @Nullable Direction direction_1, int int_1) {
            this.renderer = chunkRenderer_1;
            this.field_4125 = direction_1;
            this.field_4122 = int_1;
        }

        // $FF: synthetic method
        ChunkInfo(uk.co.xsc.securitymod.ChunkRenderer chunkRenderer_1, Direction direction_1, int int_1, Object worldRenderer$1_1) {
            this(chunkRenderer_1, direction_1, int_1);
        }

        public void method_3299(byte byte_1, Direction direction_1) {
            this.field_4126 = (byte) (this.field_4126 | byte_1 | 1 << direction_1.ordinal());
        }

        public boolean method_3298(Direction direction_1) {
            return (this.field_4126 & 1 << direction_1.ordinal()) > 0;
        }
    }
}
