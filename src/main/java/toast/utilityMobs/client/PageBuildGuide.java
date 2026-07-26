package toast.utilityMobs.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.base.PersistentData.Bookmark;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.LiquidBlockVertexConsumer;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookEye;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.client.handler.MultiblockVisualizationHandler;
import vazkii.patchouli.common.multiblock.AbstractMultiblock;
import vazkii.patchouli.common.multiblock.MultiblockRegistry;
import vazkii.patchouli.common.multiblock.SerializedMultiblock;
import vazkii.patchouli.xplat.IClientXplatAbstractions;

/**
 * Custom Patchouli page type ("utilitymobs:build_guide"). A near-verbatim copy of Patchouli's
 * PageMultiblock, but with three JSON-controllable display knobs the built-in page lacks:
 *
 * <pre>
 *   "offset_x" / "offset_y"  screen-pixel nudge of the rendered structure (default 0)
 *   "scale"                  multiplier on the auto-fit scale (default 1.0; &lt;1 adds frame margin)
 * </pre>
 *
 * <p>Defaults reproduce vanilla Patchouli rendering exactly, so untuned pages look identical to the
 * stock "multiblock" type. Lives in our own package (all Patchouli members it touches are public),
 * so no patchouli-package source injection is needed. Registered in {@link ClientSetup}.
 *
 * <p>Ported against 1.20.1's PageMultiblock, which rewrote the render for the PoseStack/buffer-source
 * pipeline. That deleted a lot of what the 1.12.2 copy had to carry: the per-tile lighting/blend/cull
 * reset, the render-pass juggling and the negative-scale culling workaround are all gone, because
 * blocks and block entities now batch into one buffer source instead of mutating global GL state.
 */
public class PageBuildGuide extends PageWithText {
    private static final RandomSource RAND = RandomSource.createNewThreadLocalInstance();

    String name = "";
    @SerializedName("multiblock_id")
    ResourceLocation multiblockId;
    @SerializedName("multiblock")
    SerializedMultiblock serializedMultiblock;
    @SerializedName("enable_visualize")
    boolean showVisualizeButton = true;

    // Display knobs (this page type's reason for existing).
    @SerializedName("offset_x")
    float offsetX = 0.0F;
    @SerializedName("offset_y")
    float offsetY = 0.0F;
    @SerializedName("scale")
    float scaleMod = 1.0F;
    // When true, any skull in the structure cycles through the mob-head types over time, signalling that
    // a colossus can be built with ANY mob head rather than the skeleton skull the pattern literally lists.
    @SerializedName("cycle_heads")
    boolean cycleHeads = false;

    // Head types we cycle through: skeleton, wither skeleton, zombie, creeper. The player head needs a
    // GameProfile and the dragon head is far too large to read in the small page frame. 1.12.2 listed
    // these as TileEntitySkull type ids { 0, 1, 2, 4 }; in 1.20.1 the head type IS the block, so the
    // cycle swaps the block state the renderer reads instead of a field on the block entity.
    private static final Block[] HEAD_TYPES = {
        Blocks.SKELETON_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.ZOMBIE_HEAD, Blocks.CREEPER_HEAD
    };
    private static final int HEAD_CYCLE_TICKS = 40;

    private transient AbstractMultiblock multiblockObj;
    private transient Button visualizeButton;
    // Hold errored BEs weakly, this may cause some dupe errors but will prevent spamming it every frame.
    private final transient Set<BlockEntity> erroredTiles = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        if (this.multiblockId != null) {
            IMultiblock mb = MultiblockRegistry.MULTIBLOCKS.get(this.multiblockId);
            if (mb instanceof AbstractMultiblock abstractMultiblock) {
                this.multiblockObj = abstractMultiblock;
            }
        }
        if (this.multiblockObj == null && this.serializedMultiblock != null) {
            this.multiblockObj = this.serializedMultiblock.toMultiblock();
        }
        if (this.multiblockObj == null) {
            throw new IllegalArgumentException("No multiblock located for " + this.multiblockId);
        }
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        if (this.showVisualizeButton) {
            this.addButton(this.visualizeButton = new GuiButtonBookEye(parent, 12, 97, this::handleButtonVisualize));
        }
    }

    @Override
    public int getTextHeight() {
        return 115;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        int x = GuiBook.PAGE_WIDTH / 2 - 53;
        int y = 7;
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiBook.drawFromTexture(graphics, this.book, x, y, 405, 149, 106, 106);
        this.parent.drawCenteredStringNoShadow(graphics, this.i18n(this.name),
                GuiBook.PAGE_WIDTH / 2, 0, this.book.headerColor);
        if (this.multiblockObj != null) {
            this.renderMultiblock(graphics);
        }
        super.render(graphics, mouseX, mouseY, pticks);
    }

    public void handleButtonVisualize(Button button) {
        ResourceLocation entryKey = this.parent.getEntry().getId();
        Bookmark bookmark = new Bookmark(entryKey, this.pageNum / 2);
        MultiblockVisualizationHandler.setMultiblock(this.multiblockObj, this.i18nText(this.name), bookmark, true);
        this.parent.addBookmarkButtons();
        if (!PersistentData.data.clickedVisualize) {
            PersistentData.data.clickedVisualize = true;
            PersistentData.save();
        }
    }

    private void renderMultiblock(GuiGraphics graphics) {
        this.multiblockObj.setWorld(this.mc.level);
        Vec3i size = this.multiblockObj.getSize();
        int sizeX = size.getX();
        int sizeY = size.getY();
        int sizeZ = size.getZ();
        float maxX = 90.0F;
        float maxY = 90.0F;
        float diag = (float) Math.sqrt(sizeX * sizeX + sizeZ * sizeZ);
        float scaleX = maxX / diag;
        float scaleY = maxY / sizeY;
        float scale = -Math.min(scaleX, scaleY) * this.scaleMod;

        float xPos = GuiBook.PAGE_WIDTH / 2.0F + this.offsetX;
        float yPos = 60.0F + this.offsetY;
        graphics.pose().pushPose();
        graphics.pose().translate(xPos, yPos, 100.0F);
        graphics.pose().scale(scale, scale, scale);
        graphics.pose().translate(-(float) sizeX / 2.0F, -(float) sizeY / 2.0F, 0.0F);

        // Initial eye pos somewhere off in the distance in the -Z direction.
        Vector4f eye = new Vector4f(0.0F, 0.0F, -100.0F, 1.0F);
        Matrix4f rotMat = new Matrix4f();
        rotMat.identity();

        // For each rotation done, track the opposite to keep the eye pos accurate.
        graphics.pose().mulPose(Axis.XP.rotationDegrees(-30.0F));
        rotMat.rotation(Axis.XP.rotationDegrees(30.0F));

        float offX = (float) -sizeX / 2.0F;
        float offZ = (float) -sizeZ / 2.0F + 1.0F;

        float time = this.parent.ticksInBook * 0.5F;
        if (!Screen.hasShiftDown()) {
            time += ClientTicker.partialTicks;
        }
        graphics.pose().translate(-offX, 0.0F, -offZ);
        graphics.pose().mulPose(Axis.YP.rotationDegrees(time));
        rotMat.rotation(Axis.YP.rotationDegrees(-time));
        graphics.pose().mulPose(Axis.YP.rotationDegrees(45.0F));
        rotMat.rotation(Axis.YP.rotationDegrees(-45.0F));
        graphics.pose().translate(offX, 0.0F, offZ);

        eye.mul(rotMat);
        this.renderElements(graphics, this.multiblockObj,
                BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(sizeX - 1, sizeY - 1, sizeZ - 1)), eye);

        graphics.pose().popPose();
    }

    private void renderElements(GuiGraphics graphics, AbstractMultiblock mb, Iterable<? extends BlockPos> blocks, Vector4f eye) {
        graphics.pose().pushPose();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().translate(0.0F, 0.0F, -1.0F);

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        this.doWorldRenderPass(graphics, mb, blocks, buffers);
        this.doTileEntityRenderPass(graphics, mb, blocks, buffers);

        buffers.endBatch();
        graphics.pose().popPose();
    }

    private void doWorldRenderPass(GuiGraphics graphics, AbstractMultiblock mb, Iterable<? extends BlockPos> blocks,
            MultiBufferSource.BufferSource buffers) {
        for (BlockPos pos : blocks) {
            BlockState bs = mb.getBlockState(pos);
            graphics.pose().pushPose();
            graphics.pose().translate(pos.getX(), pos.getY(), pos.getZ());

            FluidState fluidState = bs.getFluidState();
            BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
            if (!fluidState.isEmpty()) {
                RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluidState);
                VertexConsumer buffer = buffers.getBuffer(layer);
                blockRenderer.renderLiquid(pos, mb, new LiquidBlockVertexConsumer(buffer, graphics.pose(), pos), bs, fluidState);
            }
            IClientXplatAbstractions.INSTANCE.renderForMultiblock(bs, pos, mb, graphics.pose(), buffers, PageBuildGuide.RAND);
            graphics.pose().popPose();
        }
    }

    private void doTileEntityRenderPass(GuiGraphics graphics, AbstractMultiblock mb, Iterable<? extends BlockPos> blocks,
            MultiBufferSource buffers) {
        for (BlockPos pos : blocks) {
            BlockEntity te = mb.getBlockEntity(pos);
            if (te != null && !this.erroredTiles.contains(te)) {
                te.setLevel(this.mc.level);

                // Fake cached state in case the renderer checks it, as we don't want to query the actual
                // world. This is also where the head cycle happens: SkullBlockRenderer reads the head type
                // off the block entity's cached block state, so handing it a cycled skull state is the
                // 1.20.1 equivalent of 1.12.2's TileEntitySkull.setType().
                te.setBlockState(this.displayState(mb.getBlockState(pos)));

                graphics.pose().pushPose();
                graphics.pose().translate(pos.getX(), pos.getY(), pos.getZ());
                try {
                    BlockEntityRenderer<BlockEntity> renderer =
                            Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(te);
                    if (renderer != null) {
                        renderer.render(te, ClientTicker.partialTicks, graphics.pose(), buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
                    }
                } catch (Exception e) {
                    this.erroredTiles.add(te);
                    PatchouliAPI.LOGGER.error("An exception occured rendering tile entity", e);
                } finally {
                    graphics.pose().popPose();
                }
            }
        }
    }

    /// The block state to render, which is the pattern's own state unless the head cycle rewrites it.
    /// Only floor skulls cycle; a wall skull carries a FACING the floor variants do not have.
    private BlockState displayState(BlockState state) {
        if (!this.cycleHeads || !(state.getBlock() instanceof SkullBlock)) {
            return state;
        }
        int idx = (this.parent.ticksInBook / PageBuildGuide.HEAD_CYCLE_TICKS) % PageBuildGuide.HEAD_TYPES.length;
        Block head = PageBuildGuide.HEAD_TYPES[idx];
        if (head == state.getBlock()) {
            return state;
        }
        return head.defaultBlockState().setValue(SkullBlock.ROTATION, state.getValue(SkullBlock.ROTATION));
    }
}
