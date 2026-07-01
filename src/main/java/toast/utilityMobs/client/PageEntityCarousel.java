package toast.utilityMobs.client;

import com.google.gson.annotations.SerializedName;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.PageEntity;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.util.EntityUtil;

/**
    Custom Patchouli page type ("utilitymobs:entity_carousel"). Like the stock "entity" page but
    cycles through several entities, showing one at a time and slowly swapping between them while
    each spins. Reuses Patchouli's public PageEntity.renderEntity() for the actual draw.

    JSON: "entities" (string array of entity ids), optional "name" (title; defaults to the current
    entity's name), "scale" (default 1.0), "offset" (default 0), "interval" (ticks per entity, 60).
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

    transient Entity[] loaded;
    transient float[] renderScale;
    transient float[] offset;

    @Override
    public void build(BookEntry entry, int pageNum) {
        super.build(entry, pageNum);
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        this.loadEntities(this.mc.world);
    }

    @Override
    public int getTextHeight() {
        return 115;
    }

    private void loadEntities(World world) {
        int n = this.entities == null ? 0 : this.entities.length;
        this.loaded = new Entity[n];
        this.renderScale = new float[n];
        this.offset = new float[n];
        for (int i = 0; i < n; i++) {
            try {
                Entity e = EntityUtil.loadEntity(this.entities[i]).create(world);
                float size = Math.max(e.width, e.height);
                size = Math.max(1.0F, size);
                this.renderScale[i] = 100.0F / size * 0.8F * this.scale;
                this.offset[i] = Math.max(e.height, size) * 0.5F + this.extraOffset;
                this.loaded[i] = e;
            } catch (Exception ex) {
                ex.printStackTrace();
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
    public void render(int mouseX, int mouseY, float pticks) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        GuiBook.drawFromTexture(this.book, 5, 7, 405, 149, 106, 106);
        int idx = this.currentIndex();
        Entity e = this.loaded != null && idx < this.loaded.length ? this.loaded[idx] : null;
        String title = this.name;
        if ((title == null || title.isEmpty()) && e != null) {
            title = e.getName();
        }
        if (title != null && !title.isEmpty()) {
            this.parent.drawCenteredStringNoShadow(title, 58, 0, this.book.headerColor);
        }
        if (e != null) {
            PageEntity.renderEntity(e, this.mc.world, 58.0F, 60.0F, ClientTicker.total,
                    this.renderScale[idx], this.offset[idx]);
        }
        super.render(mouseX, mouseY, pticks);
    }
}
