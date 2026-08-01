package toast.utilityMobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.CommonProxy;
import toast.utilityMobs.EntityGolemFishHook;
import toast.utilityMobs.UMSound;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.network.MessageExplosion;
import toast.utilityMobs.block.EntityAnvilGolem;
import toast.utilityMobs.block.EntityBlockGolem;
import toast.utilityMobs.block.EntityChestGolem;
import toast.utilityMobs.block.EntityJukeboxGolem;
import toast.utilityMobs.client.model.ModelGolemPlayer;
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
import toast.utilityMobs.golem.EntityBoundSoul;
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
        // Patchouli is a soft dependency (issue #1.6). Everything that touches it lives in
        // PatchouliClientHooks, which is only loaded when Patchouli is actually present - several of those
        // types extend Patchouli classes, so naming them from here would fail without it.
        if (toast.utilityMobs.PatchouliCompat.isLoaded()) {
            PatchouliClientHooks.install();
        }
        // Cursor-capable editor for target-list books (vanilla book GUI can't move the cursor).
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GuiTargetBookEditor.OpenHandler());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TurretOutlineRenderer());
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
        // Bound souls use the player model so resource packs get the second (overlay) texture layer (#15).
        RenderingRegistry.registerEntityRenderingHandler(EntityBoundSoul.class, manager -> new RenderGolem(manager, new ModelGolemPlayer()));
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

    // Client half of the heal-number packet. The handler itself stays client-class-free so the packet
    // can also be registered (and encoded) on a dedicated server. See issue #10.
    @Override
    public void handleHealNumber(final int entityId, final float amount) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                World world = FMLClientHandler.instance().getWorldClient();
                if (world == null) {
                    return;
                }
                Entity entity = world.getEntityByID(entityId);
                if (entity != null) {
                    ClientProxy.this.spawnHealNumber(entity, amount);
                }
            }
        });
    }

    // Client half of the explosion-effect packet. Same reason as above.
    @Override
    public void handleExplosionFx(final MessageExplosion message) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientProxy.playExplosionFx(message);
            }
        });
    }

    private static void playExplosionFx(MessageExplosion message) {
        World world = FMLClientHandler.instance().getWorldClient();
        if (world == null)
            return;
        if (message.type == MessageExplosion.ExplosionType.NORMAL && message.size >= 2.0F) {
            world.spawnParticle(UMSound.HUGE_EXPLOSION, message.posX, message.posY, message.posZ, 1.0, 0.0, 0.0);
        }
        else {
            world.spawnParticle(UMSound.LARGE_EXPLODE, message.posX, message.posY, message.posZ, 1.0, 0.0, 0.0);
        }

        if (message.type == MessageExplosion.ExplosionType.NORMAL && message.affectedBlocks != null) {
            int count = message.affectedBlocks.length;
            double[] relPos;
            double fxPosX, fxPosY, fxPosZ;
            for (int i = 0; i < count; i++) {
                relPos = new double[3];
                for (int d = 0; d < 3; d++) {
                    relPos[d] = message.affectedBlocks[i][d] + world.rand.nextFloat();
                }
                fxPosX = relPos[0] + message.posX;
                fxPosY = relPos[1] + message.posY;
                fxPosZ = relPos[2] + message.posZ;
                double velo = Math.sqrt(relPos[0] * relPos[0] + relPos[1] * relPos[1] + relPos[2] * relPos[2]);
                double mult = 0.5 / (velo / message.size + 0.1) * (world.rand.nextFloat() * world.rand.nextFloat() + 0.3F) / velo;
                for (int d = 0; d < 3; d++) {
                    relPos[d] *= mult;
                }
                world.spawnParticle(UMSound.EXPLODE, (fxPosX + message.posX) / 2.0, (fxPosY + message.posY) / 2.0, (fxPosZ + message.posZ) / 2.0, relPos[0], relPos[1], relPos[2]);
                world.spawnParticle(UMSound.SMOKE, fxPosX, fxPosY, fxPosZ, relPos[0], relPos[1], relPos[2]);
            }
        }
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

}
