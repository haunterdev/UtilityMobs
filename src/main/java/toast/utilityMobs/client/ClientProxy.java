package toast.utilityMobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.CommonProxy;
import toast.utilityMobs.EntityGolemFishHook;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.block.EntityAnvilGolem;
import toast.utilityMobs.block.EntityBlockGolem;
import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.block.EntityJukeboxGolem;
import toast.utilityMobs.client.model.ModelSkeletonGolem;
import toast.utilityMobs.client.renderer.RenderAnvilGolem;
import toast.utilityMobs.client.renderer.RenderBlockGolem;
import toast.utilityMobs.client.renderer.RenderChestGolem;
import toast.utilityMobs.client.renderer.RenderColossalGolem;
import toast.utilityMobs.client.renderer.RenderGolem;
import toast.utilityMobs.client.renderer.RenderGolemFishHook;
import toast.utilityMobs.client.renderer.RenderLargeGolem;
import toast.utilityMobs.client.renderer.RenderStackGolem;
import toast.utilityMobs.client.renderer.RenderTurret;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.golem.EntityLargeGolem;
import toast.utilityMobs.golem.EntityScarecrow;
import toast.utilityMobs.golem.EntityStackGolem;
import toast.utilityMobs.golem.EntityStoneGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.network.MessageUseGolem;
import toast.utilityMobs.turret.EntityTurretGolem;
import toast.utilityMobs.turret.EntityTurretArrow;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    private int packetCooldown;

    // Returns the username of the player if this is the client side.
    @Override
    public String getPlayer() {
        EntityPlayerSP player = FMLClientHandler.instance().getClientPlayerEntity();
        return player == null ? null : player.getName();
    }

    // Registers render files if this is the client side.
    @Override
    public void registerRenderers() {
        LangFix.install();
        // Custom Patchouli page type for build guides - adds offset/scale knobs the stock
        // "multiblock" page lacks. The pageTypes map only ever gains the built-in defaults, so
        // inserting here (regardless of order vs Patchouli's own init) is safe.
        vazkii.patchouli.client.book.ClientBookRegistry.INSTANCE.pageTypes.put(
                "utilitymobs:build_guide", PageBuildGuide.class);
        vazkii.patchouli.client.book.ClientBookRegistry.INSTANCE.pageTypes.put(
                "utilitymobs:entity_carousel", PageEntityCarousel.class);
        // Adds the $(lc:category) book-text link so entries can point at a whole category grid.
        UMPatchouliLinks.register();
        // Fit all 12 turret entries on the category landing page (Patchouli caps it at 11, spilling
        // the Killer turret onto a second page). Swaps the stock GUI for our 12-entry variant.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                new vazkii.patchouli.client.book.gui.GuiTurretCategory.OpenHandler());
        // Cursor-capable editor for target-list books (vanilla book GUI can't move the cursor).
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GuiTargetBookEditor.OpenHandler());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TurretOutlineRenderer());
        // Dismiss Patchouli's projected multiblock ghost once the golem actually spawns (our builds
        // become an entity the instant they complete, so Patchouli's own "all blocks present" clear
        // never fires). See MultiblockBuildClearHandler.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MultiblockBuildClearHandler());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new HealTextRenderer());
        // Draws the turret GUI's "?" help tooltip after JEI's overlay so it isn't hidden behind JEI items.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GuiTurretGolem.HelpTooltipHandler());
        // First-launch setup wizard: swaps the main menu for the experience picker until dismissed.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SetupWizardHandler());
        RenderingRegistry.registerEntityRenderingHandler(EntityGolemFishHook.class, RenderGolemFishHook::new);

        RenderingRegistry.registerEntityRenderingHandler(EntityUtilityGolem.class, RenderGolem::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLargeGolem.class, RenderLargeGolem::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityStackGolem.class, RenderStackGolem::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityScarecrow.class, manager -> new RenderGolem(manager, new ModelSkeletonGolem()));
        RenderingRegistry.registerEntityRenderingHandler(EntityStoneGolem.class, manager -> new RenderGolem(manager, new ModelZombie(0.0F, true)));

        RenderingRegistry.registerEntityRenderingHandler(EntityTurretGolem.class, RenderTurret::new);
        // Turret arrows render with the vanilla arrow renderer (no potion tint; they're untinted tipped arrows).
        RenderingRegistry.registerEntityRenderingHandler(EntityTurretArrow.class, net.minecraft.client.renderer.entity.RenderTippedArrow::new);

        RenderingRegistry.registerEntityRenderingHandler(EntityBlockGolem.class, RenderBlockGolem::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityAnvilGolem.class, RenderAnvilGolem::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityChestGolem.class, RenderChestGolem::new);

        RenderingRegistry.registerEntityRenderingHandler(EntityColossalGolem.class, RenderColossalGolem::new);
    }

    // Registers the model for the hidden creative-tab icon item (stone golem face).
    @Override
    public void registerTabIconModel() {
        ModelLoader.setCustomModelResourceLocation(_UtilityMobs.TAB_ICON, 0,
                new ModelResourceLocation(_UtilityMobs.MODID + ":stonegolem_face", "inventory"));
    }

    // Plays a record for the jukebox golem.
    @Override
    public void playRecordGolem(EntityJukeboxGolem golem, String record) {
        if ("".equals(record))
            return;
        try {
            Minecraft client = FMLClientHandler.instance().getClient();
            SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(record));
            if (sound == null) {
                sound = SoundEvent.REGISTRY.getObject(new ResourceLocation("record." + record));
            }
            if (sound == null) {
                return;
            }
            ItemRecord itemRecord = ItemRecord.getBySound(sound);
            if (itemRecord != null) {
                client.ingameGUI.setRecordPlayingMessage(itemRecord.getRecordNameLocal());
            }
            MovingSoundRecord recordSound = new MovingSoundRecord(golem, record, sound);
            client.getSoundHandler().playSound(recordSound);
        }
        catch (Exception ex) {
            _UtilityMobs.console("[WARNING] Unable to play record \"", record, "\" in record golem!");
            ex.printStackTrace();
        }
    }

    @Override
    public void spawnHealNumber(Entity entity, float amount) {
        HealTextRenderer.add(entity, amount);
    }

    // Called at the end of each client tick.
    @Override
    public void handleClientTick() {
        if (this.packetCooldown > 0) {
            this.packetCooldown--;
        }
        else {
            EntityPlayerSP player = FMLClientHandler.instance().getClientPlayerEntity();
            if (player != null && Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown()
                    && player.getRidingEntity() instanceof EntityColossalGolem
                    && ((EntityColossalGolem) player.getRidingEntity()).getAnimId() == 0) {
                this.packetCooldown = 10;
                _UtilityMobs.CHANNEL.sendToServer(new MessageUseGolem());
            }
        }
    }

    // Returns true if entities are allowed to block movement. Namely, if they can be stood on.
    @Override
    public boolean solidEntities() {
        return FMLClientHandler.instance().getServer() != null;
    }
}
