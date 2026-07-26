package toast.utilityMobs.client;

import java.util.function.Function;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.PageEntity;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.util.EntityUtil;

/**
 * Custom Patchouli page type ("utilitymobs:entity_carousel"). Like the stock "entity" page but
 * cycles through several entities, showing one at a time and slowly swapping between them while
 * each spins. Reuses Patchouli's public PageEntity.renderEntity() for the actual draw.
 *
 * <p>JSON: "entities" (string array of entity ids), optional "name" (title; defaults to the current
 * entity's name), "scale" (default 1.0), "offset" (default 0), "interval" (ticks per entity, 60).
 */
public class PageEntityCarousel extends PageWithText {

    @SerializedName("entities")
    String[] entities;
    float scale = 1.0F;
    @SerializedName("offset")
    float extraOffset = 0.0F;
    @SerializedName("interval")
    int interval = 60;
    String name;

    // 1.12.2 resolved the id to an EntityEntry at build time and called create(world) per load; the
    // 1.20.1 EntityUtil hands back the same thing as a Level -> Entity function.
    transient Function<Level, Entity>[] creators;
    transient Entity[] loaded;
    transient float[] renderScale;
    transient float[] offset;

    @SuppressWarnings("unchecked")
    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        int n = this.entities == null ? 0 : this.entities.length;
        this.creators = new Function[n];
        for (int i = 0; i < n; i++) {
            this.creators[i] = EntityUtil.loadEntity(this.entities[i]);
        }
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        this.loadEntities(parent.getMinecraft().level);
    }

    @Override
    public int getTextHeight() {
        return 115;
    }

    private void loadEntities(Level world) {
        int n = this.creators == null ? 0 : this.creators.length;
        this.loaded = new Entity[n];
        this.renderScale = new float[n];
        this.offset = new float[n];
        for (int i = 0; i < n; i++) {
            try {
                Entity e = this.creators[i].apply(world);
                float width = e.getBbWidth();
                float height = e.getBbHeight();
                float size = Math.max(1.0F, Math.max(width, height));
                this.renderScale[i] = 100.0F / size * 0.8F * this.scale;
                this.offset[i] = Math.max(height, size) * 0.5F + this.extraOffset;
                this.loaded[i] = e;
            } catch (Exception ex) {
                vazkii.patchouli.api.PatchouliAPI.LOGGER.error("Failed to load carousel entity", ex);
            }
        }
    }

    private int currentIndex() {
        if (this.loaded == null || this.loaded.length == 0) {
            return 0;
        }
        int step = Math.max(1, this.interval);
        return (int) (ClientTicker.total / step) % this.loaded.length;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiBook.drawFromTexture(graphics, this.book, GuiBook.PAGE_WIDTH / 2 - 53, 7, 405, 149, 106, 106);
        // Deliberately no enableBlend here. Blend left on leaks into the entity draw below, and any
        // solid geometry an entity's layers render through their own render type - the scarecrow's
        // carved pumpkin via CustomHeadLayer is the visible case - comes out see-through. Vanilla's
        // inventory entity render and Patchouli's own PageEntity both leave blend to the render types.
        int idx = this.currentIndex();
        Entity e = this.loaded != null && idx < this.loaded.length ? this.loaded[idx] : null;
        if (this.name != null && !this.name.isEmpty()) {
            this.parent.drawCenteredStringNoShadow(graphics, this.i18n(this.name),
                    GuiBook.PAGE_WIDTH / 2, 0, this.book.headerColor);
        }
        else if (e != null) {
            this.parent.drawCenteredStringNoShadow(graphics, e.getName().getVisualOrderText(),
                    GuiBook.PAGE_WIDTH / 2, 0, this.book.headerColor);
        }
        if (e != null) {
            PageEntity.renderEntity(graphics, e, 58.0F, 60.0F, ClientTicker.total,
                    this.renderScale[idx], this.offset[idx]);
        }
        super.render(graphics, mouseX, mouseY, pticks);
    }
}
