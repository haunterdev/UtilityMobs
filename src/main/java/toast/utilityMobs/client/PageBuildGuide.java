package toast.utilityMobs.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import com.google.gson.annotations.SerializedName;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;

import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookEye;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.client.handler.MultiblockVisualizationHandler;
import vazkii.patchouli.common.multiblock.Multiblock;
import vazkii.patchouli.common.multiblock.MultiblockRegistry;
import vazkii.patchouli.common.multiblock.SerializedMultiblock;

/**
    Custom Patchouli page type ("utilitymobs:build_guide"). A near-verbatim copy of Patchouli's
    PageMultiblock, but with three JSON-controllable display knobs the built-in page lacks:

      "offset_x" / "offset_y"  screen-pixel nudge of the rendered structure (default 0)
      "scale"                  multiplier on the auto-fit scale (default 1.0; <1 adds frame margin)

    Defaults reproduce vanilla Patchouli rendering exactly, so untuned pages look identical to the
    stock "multiblock" type. Lives in our own package (all Patchouli members it touches are public),
    so no patchouli-package source injection is needed. Registered in ClientProxy.
 */
public class PageBuildGuide extends PageWithText {

    String name;
    @SerializedName("multiblock_id")
    String multiblockId;
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

    // Head types we cycle through (TileEntitySkull): skeleton, wither skeleton, zombie, creeper. Player(3)
    // needs a GameProfile and the dragon(5) head is far too large to read in the small page frame.
    private static final int[] HEAD_TYPES = { 0, 1, 2, 4 };
    private static final int HEAD_CYCLE_TICKS = 40;

    transient Multiblock multiblockObj;
    transient GuiButton visualizeButton;
    private final transient Set<TileEntity> erroredTiles = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void build(BookEntry entry, int pageNum) {
        super.build(entry, pageNum);
        if (this.multiblockId != null && !this.multiblockId.isEmpty()) {
            IMultiblock mb = MultiblockRegistry.MULTIBLOCKS.get(new ResourceLocation(this.multiblockId));
            if (mb instanceof Multiblock) {
                this.multiblockObj = (Multiblock) mb;
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
            this.visualizeButton = new GuiButtonBookEye(parent, 12, 97);
            this.addButton(this.visualizeButton);
        }
    }

    @Override
    public int getTextHeight() {
        return 115;
    }

    @Override
    public void render(int mouseX, int mouseY, float pticks) {
        int x = 5;
        int y = 7;
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        GuiBook.drawFromTexture(this.book, x, y, 405, 149, 106, 106);
        this.parent.drawCenteredStringNoShadow(this.name, 58, 0, this.book.headerColor);
        if (this.multiblockObj != null) {
            this.renderMultiblock();
        }
        super.render(mouseX, mouseY, pticks);
    }

    @Override
    protected void onButtonClicked(GuiButton button) {
        if (button == this.visualizeButton) {
            String entryKey = this.parent.getEntry().getResource().toString();
            PersistentData.DataHolder.BookData.Bookmark bookmark =
                    new PersistentData.DataHolder.BookData.Bookmark(entryKey, this.pageNum / 2);
            MultiblockVisualizationHandler.setMultiblock(this.multiblockObj, this.name, bookmark, true);
            this.parent.addBookmarkButtons();
            if (!PersistentData.data.clickedVisualize) {
                PersistentData.data.clickedVisualize = true;
                PersistentData.save();
            }
        }
    }

    private void renderMultiblock() {
        float maxX = 90.0F;
        float maxY = 90.0F;
        float diag = (float) Math.sqrt(this.multiblockObj.sizeX * this.multiblockObj.sizeX
                + this.multiblockObj.sizeZ * this.multiblockObj.sizeZ);
        float height = this.multiblockObj.sizeY;
        float scaleX = maxX / diag;
        float scaleY = maxY / height;
        float scale = -Math.min(scaleX, scaleY) * this.scaleMod;
        float xPos = 58.0F + this.offsetX;
        float yPos = 60.0F + this.offsetY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(xPos, yPos, 100.0F);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(-((float) this.multiblockObj.sizeX) / 2.0F,
                -((float) this.multiblockObj.sizeY) / 2.0F, 0.0F);
        Vector4f eye = new Vector4f(0.0F, 0.0F, -100.0F, 1.0F);
        Matrix4f rotMat = new Matrix4f();
        rotMat.setIdentity();
        GlStateManager.rotate(-30.0F, 1.0F, 0.0F, 0.0F);
        rotMat.rotX((float) Math.toRadians(30.0));
        float offX = (float) (-this.multiblockObj.sizeX) / 2.0F;
        float offZ = (float) (-this.multiblockObj.sizeZ) / 2.0F + 1.0F;
        float time = this.parent.ticksInBook * 0.5F;
        if (!GuiScreen.isShiftKeyDown()) {
            time += ClientTicker.partialTicks;
        }
        GlStateManager.translate(-offX, 0.0F, -offZ);
        GlStateManager.rotate(time, 0.0F, 1.0F, 0.0F);
        rotMat.rotY((float) Math.toRadians(-time));
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        rotMat.rotY((float) Math.toRadians(-45.0));
        GlStateManager.translate(offX, 0.0F, offZ);
        rotMat.transform(eye);
        this.renderElements(this.multiblockObj,
                BlockPos.getAllInBoxMutable(BlockPos.ORIGIN, new BlockPos(this.multiblockObj.sizeX - 1,
                        this.multiblockObj.sizeY - 1, this.multiblockObj.sizeZ - 1)), eye);
        GlStateManager.popMatrix();
    }

    private void renderElements(Multiblock mb, Iterable<? extends BlockPos> blocks, Vector4f eye) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate(0.0F, 0.0F, -1.0F);
        TileEntityRendererDispatcher.instance.entityX = eye.x;
        TileEntityRendererDispatcher.instance.entityY = eye.y;
        TileEntityRendererDispatcher.instance.entityZ = eye.z;
        TileEntityRendererDispatcher.staticPlayerX = eye.x;
        TileEntityRendererDispatcher.staticPlayerY = eye.y;
        TileEntityRendererDispatcher.staticPlayerZ = eye.z;
        BlockRenderLayer oldRenderLayer = MinecraftForgeClient.getRenderLayer();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (layer == BlockRenderLayer.TRANSLUCENT) {
                this.doTileEntityRenderPass(mb, blocks, 0);
            }
            this.doWorldRenderPass(mb, blocks, layer, eye);
            if (layer == BlockRenderLayer.TRANSLUCENT) {
                this.doTileEntityRenderPass(mb, blocks, 1);
            }
        }
        ForgeHooksClient.setRenderLayer(oldRenderLayer);
        ForgeHooksClient.setRenderPass(-1);
        this.setGlStateForPass(0);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        GlStateManager.popMatrix();
    }

    private void doWorldRenderPass(Multiblock mb, Iterable<? extends BlockPos> blocks, BlockRenderLayer layer, Vector4f eye) {
        this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        ForgeHooksClient.setRenderLayer(layer);
        this.setGlStateForPass(layer);
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(7, DefaultVertexFormats.BLOCK);
        for (BlockPos pos : blocks) {
            IBlockState bs = mb.getBlockState(pos);
            Block block = bs.getBlock();
            if (!block.canRenderInLayer(bs = bs.getActualState(mb, pos), layer)) {
                continue;
            }
            this.renderBlock(bs, pos, mb, Tessellator.getInstance().getBuffer());
        }
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            wr.sortVertexData(eye.x, eye.y, eye.z);
        }
        Tessellator.getInstance().draw();
    }

    private void renderBlock(IBlockState state, BlockPos pos, Multiblock mb, BufferBuilder buffer) {
        try {
            BlockRendererDispatcher brd = this.mc.getBlockRendererDispatcher();
            EnumBlockRenderType type = state.getRenderType();
            if (type != EnumBlockRenderType.MODEL) {
                brd.renderBlock(state, pos, mb, buffer);
                return;
            }
            IBakedModel model = brd.getModelForState(state);
            state = state.getBlock().getExtendedState(state, mb, pos);
            brd.getBlockModelRenderer().renderModel(mb, model, state, pos, buffer, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doTileEntityRenderPass(Multiblock mb, Iterable<? extends BlockPos> blocks, int pass) {
        mb.setWorld(this.mc.world);
        ForgeHooksClient.setRenderPass(1);
        for (BlockPos pos : blocks) {
            TileEntity te = mb.getTileEntity(pos);
            BlockPos relPos = new BlockPos(this.mc.player);
            if (te == null || this.erroredTiles.contains(te) || !te.shouldRenderInPass(pass)) {
                continue;
            }
            // Re-establish a clean render state before EACH tile entity. A preceding TESR (the
            // chest golem stacks a chest's TileEntityChestRenderer below the skull) leaves blend,
            // culling, lighting and color altered, which made the skull on top render opaque
            // instead of matching the translucent preview the way every other golem's skull does.
            // Resetting per-iteration makes all skulls render identically regardless of neighbours.
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableLighting();
            // The negative scale in renderMultiblock() flips face winding; with culling on, a TESR
            // skull's front faces get culled and you see into the hollow interior. Render both faces
            // (depth still keeps the nearest) so heads render solid.
            GlStateManager.disableCull();
            this.setGlStateForPass(1);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            te.setWorld(this.mc.world);
            te.setPos(relPos.add(pos));
            if (this.cycleHeads && te instanceof TileEntitySkull) {
                int idx = (this.parent.ticksInBook / HEAD_CYCLE_TICKS) % HEAD_TYPES.length;
                ((TileEntitySkull) te).setType(HEAD_TYPES[idx]);
            }
            try {
                TileEntityRendererDispatcher.instance.render(te, pos.getX(), pos.getY(), pos.getZ(),
                        ClientTicker.partialTicks);
            } catch (Exception e) {
                this.erroredTiles.add(te);
                e.printStackTrace();
            }
        }
        ForgeHooksClient.setRenderPass(-1);
        GlStateManager.enableCull();
        RenderHelper.disableStandardItemLighting();
    }

    private void setGlStateForPass(BlockRenderLayer layer) {
        int pass = layer == BlockRenderLayer.TRANSLUCENT ? 1 : 0;
        this.setGlStateForPass(pass);
    }

    private void setGlStateForPass(int layer) {
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        if (layer == 0) {
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        } else {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.depthMask(false);
        }
    }
}
