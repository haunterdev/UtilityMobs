package toast.utilityMobs.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.block.ContainerLanternGolem;
import toast.utilityMobs.client.model.ModelAnvilGolem;
import toast.utilityMobs.client.model.ModelBlockGolem;
import toast.utilityMobs.client.model.ModelChestGolem;
import toast.utilityMobs.client.model.ModelColossalGolem;
import toast.utilityMobs.client.model.ModelGolemBiped;
import toast.utilityMobs.client.model.ModelLargeGolem;
import toast.utilityMobs.client.model.ModelSkeletonGolem;
import toast.utilityMobs.client.model.ModelStackGolem;
import toast.utilityMobs.client.renderer.RenderAnvilGolem;
import toast.utilityMobs.client.renderer.RenderBlockGolem;
import toast.utilityMobs.client.renderer.RenderChestGolem;
import toast.utilityMobs.client.renderer.RenderColossalGolem;
import toast.utilityMobs.client.renderer.RenderGolemFishHook;
import toast.utilityMobs.client.renderer.RenderLargeGolem;
import toast.utilityMobs.client.renderer.RenderStackGolem;
import toast.utilityMobs.client.model.ModelTurret;
import toast.utilityMobs.client.renderer.RenderGolem;
import toast.utilityMobs.client.renderer.RenderTurret;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.setup.ModEntities;

/**
 * Client-side registration. Replaces 1.12.2's ClientProxy: model layers are baked once by the
 * layer definition registry, then renderers pull the baked parts out of the render context.
 */
@Mod.EventBusSubscriber(modid = _UtilityMobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}

    public static final ModelLayerLocation TURRET_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "turret"), "main");
    public static final ModelLayerLocation GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "golem"), "main");
    public static final ModelLayerLocation BLOCK_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "block_golem"), "main");
    public static final ModelLayerLocation LARGE_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "large_golem"), "main");
    public static final ModelLayerLocation STACK_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "stack_golem"), "main");
    public static final ModelLayerLocation CHEST_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "chest_golem"), "main");
    public static final ModelLayerLocation COLOSSAL_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "colossal_golem"), "main");
    public static final ModelLayerLocation SKELETON_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "skeleton_golem"), "main");
    public static final ModelLayerLocation ANVIL_GOLEM_LAYER =
        new ModelLayerLocation(new ResourceLocation(_UtilityMobs.MODID, "anvil_golem"), "main");

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TURRET_LAYER, ModelTurret::createBodyLayer);
        event.registerLayerDefinition(GOLEM_LAYER, ModelGolemBiped::createBodyLayer);
        event.registerLayerDefinition(BLOCK_GOLEM_LAYER, ModelBlockGolem::createBodyLayer);
        event.registerLayerDefinition(LARGE_GOLEM_LAYER, ModelLargeGolem::createBodyLayer);
        event.registerLayerDefinition(STACK_GOLEM_LAYER, ModelStackGolem::createBodyLayer);
        event.registerLayerDefinition(CHEST_GOLEM_LAYER, ModelChestGolem::createBodyLayer);
        event.registerLayerDefinition(COLOSSAL_GOLEM_LAYER, ModelColossalGolem::createBodyLayer);
        event.registerLayerDefinition(SKELETON_GOLEM_LAYER, ModelSkeletonGolem::createBodyLayer);
        event.registerLayerDefinition(ANVIL_GOLEM_LAYER, ModelAnvilGolem::createBodyLayer);
    }

    /// The default humanoid golem renderer, on the shared 64x32 biped layer. Typed to the base class
    /// so every golem subtype can register with it.
    private static EntityRendererProvider<EntityUtilityGolem> biped() {
        return ctx -> new RenderGolem(ctx, ModelGolemBiped.create(ctx.bakeLayer(GOLEM_LAYER)));
    }

    /// The thin-limbed variant of the humanoid renderer, on its own 64x32 layer.
    private static EntityRendererProvider<EntityUtilityGolem> skeletonBiped() {
        return ctx -> new RenderGolem(ctx, new ModelSkeletonGolem(ctx.bakeLayer(SKELETON_GOLEM_LAYER)));
    }

    /// Screens for the menus this mod draws itself. Vanilla-typed menus (anvil, furnace, crafting)
    /// already have their screens registered by vanilla.
    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens
            .<ContainerLanternGolem, GuiGenericInventory<ContainerLanternGolem>>register(
                toast.utilityMobs.setup.ModMenus.LANTERN_GOLEM.get(),
                (menu, inventory, title) -> new GuiGenericInventory<>(menu, inventory, title, GuiGenericInventory.TEXTURE_DISPENSER)));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
            toast.utilityMobs.setup.ModMenus.TURRET_GOLEM.get(), GuiTurretGolem::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
            toast.utilityMobs.setup.ModMenus.STEAM_GOLEM.get(), GuiSteamGolem::new));

        // Client-side Forge-bus subscribers. 1.12.2 registered these in ClientProxy.preInit.
        event.enqueueWork(() -> {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SetupWizardHandler());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GuiTargetBookEditor.OpenHandler());
            // Patchouli is optional; only now is it safe to load anything naming its internals.
            if (net.minecraftforge.fml.ModList.get().isLoaded("patchouli")) {
                UMPatchouli.init();
            }
            // Puts a Config button on this mod's row in the mod list. This is 1.20.1's replacement for
            // 1.12.2's mcmod.info "guiFactory" key, which pointed at toast.utilityMobs.client.GuiFactory.
            net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new GuiUtilityMobsConfig(parent)));
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STONE_GOLEM.get(), biped());
        event.registerEntityRenderer(ModEntities.ARMOR_GOLEM.get(), biped());
        event.registerEntityRenderer(ModEntities.BOUND_SOUL.get(), biped());
        event.registerEntityRenderer(ModEntities.GILDED_GOLEM.get(), biped());
        // The scarecrow is the one golem 1.12.2 gave skeleton limbs to.
        event.registerEntityRenderer(ModEntities.SCARECROW.get(), skeletonBiped());
        // Snowman-shaped golems (stack, melon, UM snow) use the vanilla snow-golem geometry.
        event.registerEntityRenderer(ModEntities.STACK_GOLEM.get(), RenderStackGolem::new);
        event.registerEntityRenderer(ModEntities.MELON_GOLEM.get(), RenderStackGolem::new);
        event.registerEntityRenderer(ModEntities.UM_SNOW_GOLEM.get(), RenderStackGolem::new);
        // Workbench golem uses the cube ModelBlockGolem (64x32 workbenchgolem.png).
        event.registerEntityRenderer(ModEntities.WORKBENCH_GOLEM.get(), RenderBlockGolem::new);
        event.registerEntityRenderer(ModEntities.FURNACE_GOLEM.get(), RenderBlockGolem::new);
        event.registerEntityRenderer(ModEntities.JUKEBOX_GOLEM.get(), RenderBlockGolem::new);
        event.registerEntityRenderer(ModEntities.LANTERN_GOLEM.get(), RenderBlockGolem::new);
        // The anvil golem has its own four-legged model.
        event.registerEntityRenderer(ModEntities.ANVIL_GOLEM.get(), RenderAnvilGolem::new);
        // Chest golems use the 64x64 ModelChestGolem (hinged-lid chest body on four legs).
        event.registerEntityRenderer(ModEntities.CHEST_ENDER_GOLEM.get(), RenderChestGolem::new);
        event.registerEntityRenderer(ModEntities.CHEST_GOLEM.get(), RenderChestGolem::new);
        event.registerEntityRenderer(ModEntities.CHEST_TRAPPED_GOLEM.get(), RenderChestGolem::new);
        // Large golems (128x128 ModelLargeGolem) with the lumbering walk sway.
        event.registerEntityRenderer(ModEntities.OBSIDIAN_GOLEM.get(), RenderLargeGolem::new);
        event.registerEntityRenderer(ModEntities.STONE_LARGE_GOLEM.get(), RenderLargeGolem::new);
        event.registerEntityRenderer(ModEntities.UM_IRON_GOLEM.get(), RenderLargeGolem::new);
        event.registerEntityRenderer(ModEntities.STEAM_GOLEM.get(), RenderLargeGolem::new);
        // Colossal golems (128x128 ModelColossalGolem), rendered at 1.35x with the lumbering sway.
        event.registerEntityRenderer(ModEntities.ARMOR_COLOSSUS.get(), RenderColossalGolem::new);
        event.registerEntityRenderer(ModEntities.OBSIDIAN_COLOSSUS.get(), RenderColossalGolem::new);
        event.registerEntityRenderer(ModEntities.STONE_COLOSSUS.get(), RenderColossalGolem::new);
        event.registerEntityRenderer(ModEntities.STONE_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.BRICK_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.FIREBALL_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.FIRE_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.GATLING_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.GHAST_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.KILLER_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.OBSIDIAN_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.SHOTGUN_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.SNIPER_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.SNOW_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.VOLLEY_TURRET.get(), RenderTurret::new);
        event.registerEntityRenderer(ModEntities.GOLEM_FISH_HOOK.get(), RenderGolemFishHook::new);
        // Turret arrows render with the vanilla tipped-arrow renderer, as in 1.12.2.
        event.registerEntityRenderer(ModEntities.TURRET_ARROW.get(), TippableArrowRenderer::new);
    }
}
