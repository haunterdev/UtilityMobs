package toast.utilityMobs.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import toast.utilityMobs.TargetHelper;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.golem.EntityBoundSoul;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.setup.ModEntities;

/**
 * Issues #19, #20 and #21, checked on a server with a mock survival player.
 *
 * <p>Each test runs its interactions synchronously inside one tick, so sounds captured from the Forge
 * sound event belong to that interaction and nothing else running in the batch. Run with
 * {@code gradlew runGameTestServer}; inert in a normal game.
 */
@GameTestHolder(_UtilityMobs.MODID)
public class IssueSweepTests {

    private static final String MOCK_NAME = "test-mock-player";

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) {
            helper.fail(message);
        }
    }

    /// Runs the action and returns every sound the server played while it ran.
    private static List<SoundEvent> soundsDuring(Runnable action) {
        List<SoundEvent> heard = new ArrayList<>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getSound() != null) {
                heard.add(event.getSound().value());
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            action.run();
        }
        finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        return heard;
    }

    // ---- #19: mount a colossus from anywhere on its model ----

    /// An empty main hand mounts from the ground, a held item on the ground does not (it goes on to the
    /// heal and lead interactions), and a jump-click mounts with anything in hand.
    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16")
    public static void colossusMountRules(GameTestHelper helper) {
        floor(helper);
        EntityColossalGolem colossus = helper.spawn(ModEntities.STONE_COLOSSUS.get(), new Vec3(8.0, 1.0, 8.0));
        colossus.setOwner(MOCK_NAME);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(colossus.getX() + 2.0, colossus.getY(), colossus.getZ());

        player.setOnGround(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        colossus.interact(player, InteractionHand.MAIN_HAND);
        check(helper, player.getVehicle() == colossus, "#19: an empty hand on the ground did not mount the colossus");
        player.stopRiding();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        colossus.interact(player, InteractionHand.MAIN_HAND);
        check(helper, player.getVehicle() == null, "#19: a held stick on the ground mounted the colossus");

        player.setOnGround(false);
        colossus.interact(player, InteractionHand.MAIN_HAND);
        check(helper, player.getVehicle() == colossus, "#19: a jump-click with a stick did not mount the colossus");
        player.stopRiding();
        helper.succeed();
    }

    /// The crosshair pick uses the bounding box grown by getPickRadius. A ray that passes 1.2 blocks from
    /// the colossus centre (outside the 1.8-wide collision box, inside the 1.35x model's arms) must pick
    /// it; a ray 1.6 blocks out must not.
    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16")
    public static void colossusPickCoversArms(GameTestHelper helper) {
        floor(helper);
        EntityColossalGolem colossus = helper.spawn(ModEntities.STONE_COLOSSUS.get(), new Vec3(8.0, 1.0, 8.0));
        colossus.setNoAi(true);
        Player player = helper.makeMockSurvivalPlayer();
        double half = colossus.getBbWidth() / 2.0;
        check(helper, half < 1.2, "#19: collision box already reaches 1.2 blocks, the pick test proves nothing");
        Vec3 c = colossus.position();
        double y = c.y + colossus.getBbHeight() * 0.75;
        EntityHitResult arm = pick(player, new Vec3(c.x + 4.0, y, c.z + 1.2), new Vec3(c.x - 4.0, y, c.z + 1.2));
        check(helper, arm != null && arm.getEntity() == colossus, "#19: a click 1.2 blocks from the centre (on the arm) missed the colossus");
        EntityHitResult wide = pick(player, new Vec3(c.x + 4.0, y, c.z + 1.6), new Vec3(c.x - 4.0, y, c.z + 1.6));
        check(helper, wide == null || wide.getEntity() != colossus, "#19: a click 1.6 blocks out still picked the colossus");
        helper.succeed();
    }

    private static EntityHitResult pick(Player player, Vec3 from, Vec3 to) {
        player.setPos(from.x, from.y - player.getEyeHeight(), from.z);
        AABB sweep = new AABB(from, to).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(player, from, to, sweep, e -> e instanceof EntityColossalGolem, from.distanceToSqr(to));
    }

    // ---- #20: attack_whitelist and wild tameable mobs ----

    /// With players off-limits (passive_to_players), a wild tameable mob on the global attack whitelist is
    /// a valid target; a tamed one with a player owner stays protected.
    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16")
    public static void wildTameableOnWhitelistIsTargeted(GameTestHelper helper) {
        floor(helper);
        Wolf wild = helper.spawn(EntityType.WOLF, new Vec3(4.0, 1.0, 4.0));
        Wolf tame = helper.spawn(EntityType.WOLF, new Vec3(10.0, 1.0, 10.0));
        tame.setTame(true);
        tame.setOwnerUUID(UUID.randomUUID());
        check(helper, wild.getOwnerUUID() == null, "#20: the wild wolf spawned with an owner");

        boolean passiveBefore = TargetHelper.passiveToPlayers;
        HashSet<String> ids = globalWhitelistIds();
        boolean added = ids.add("minecraft:wolf");
        try {
            TargetHelper.passiveToPlayers = true;
            TargetHelper targets = TargetHelper.getTargetHelper("um_gametest_owner");
            check(helper, targets.isValidTarget(wild), "#20: a wild wolf on attack_whitelist was rejected");
            check(helper, !targets.isValidTarget(tame), "#20: a player's tamed wolf was targeted with players off-limits");
        }
        finally {
            TargetHelper.passiveToPlayers = passiveBefore;
            if (added) {
                ids.remove("minecraft:wolf");
            }
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static HashSet<String> globalWhitelistIds() {
        try {
            Field field = TargetHelper.class.getDeclaredField("GLOBAL_WHITELIST_IDS");
            field.setAccessible(true);
            return (HashSet<String>) field.get(null);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("TargetHelper.GLOBAL_WHITELIST_IDS not found", e);
        }
    }

    // ---- #21: heal sounds, and a bound soul at full health ----

    /// A bound soul at full health leaves the food in the player's hand and plays nothing; hurt, it eats
    /// one item, heals and plays the eating sound.
    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16")
    public static void boundSoulEatsOnlyWhenHurt(GameTestHelper helper) {
        floor(helper);
        EntityBoundSoul soul = helper.spawn(ModEntities.BOUND_SOUL.get(), new Vec3(8.0, 1.0, 8.0));
        soul.setNoAi(true);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 3));

        List<SoundEvent> full = soundsDuring(() -> soul.interact(player, InteractionHand.MAIN_HAND));
        check(helper, player.getMainHandItem().getCount() == 3, "#21: a soul at full health ate the bread (" + player.getMainHandItem().getCount() + " left of 3)");
        check(helper, !full.contains(SoundEvents.GENERIC_EAT), "#21: a soul at full health played the eating sound");

        soul.setHealth(soul.getMaxHealth() - 6.0F);
        float before = soul.getHealth();
        List<SoundEvent> hurt = soundsDuring(() -> soul.interact(player, InteractionHand.MAIN_HAND));
        check(helper, player.getMainHandItem().getCount() == 2, "#21: a hurt soul did not eat one bread (" + player.getMainHandItem().getCount() + " left of 3)");
        check(helper, soul.getHealth() > before, "#21: a hurt soul did not heal from bread");
        check(helper, hurt.contains(SoundEvents.GENERIC_EAT), "#21: a hurt soul healed without the eating sound, heard " + hurt);
        check(helper, !hurt.contains(SoundEvents.IRON_GOLEM_REPAIR), "#21: a soul played the golem repair sound");
        helper.succeed();
    }

    /// A golem healed with its repair item plays the iron golem repair sound.
    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16")
    public static void golemHealPlaysRepairSound(GameTestHelper helper) {
        floor(helper);
        EntityUtilityGolem golem = helper.spawn(ModEntities.STONE_GOLEM.get(), new Vec3(8.0, 1.0, 8.0));
        golem.setNoAi(true);
        Item repair = repairItem(golem);
        check(helper, repair != null, "#21: the stone golem has no repair item");
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(repair, 2));
        golem.setHealth(golem.getMaxHealth() / 2.0F);
        float before = golem.getHealth();

        List<SoundEvent> heard = soundsDuring(() -> golem.interact(player, InteractionHand.MAIN_HAND));
        check(helper, golem.getHealth() > before, "#21: the golem did not heal from " + repair);
        check(helper, heard.contains(SoundEvents.IRON_GOLEM_REPAIR), "#21: the golem healed without the iron golem repair sound, heard " + heard);
        check(helper, player.getMainHandItem().getCount() == 1, "#21: healing did not use exactly one " + repair);
        helper.succeed();
    }

    private static Item repairItem(EntityUtilityGolem golem) {
        try {
            Method method = EntityUtilityGolem.class.getDeclaredMethod("getRepairItem");
            method.setAccessible(true);
            return (Item) method.invoke(golem);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("EntityUtilityGolem.getRepairItem not callable", e);
        }
    }
}
