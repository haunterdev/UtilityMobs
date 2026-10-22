package toast.utilityMobs.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import toast.utilityMobs._UtilityMobs;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.golem.EntityStoneGolem;
import toast.utilityMobs.setup.ModEntities;

/**
 * Colossi stay on the ground (1.20.1 issue #23: "the colossi start floating in mid-air, sometimes rising
 * indefinitely").
 *
 * <p>Each test builds a stone floor in an empty 16 x 10 x 16 arena, puts one or more colossi in a
 * situation that could lift them (alone, touching, overlapping, carrying a golem, at a step, in water,
 * fighting), lets them act for 200 ticks and fails if any colossus ever rose further above its floor
 * than the scenario allows. The highest point each colossus reached is logged, so a pass still says how
 * close it came.
 *
 * <p>Run with {@code gradlew runGameTestServer}; inert in a normal game.
 */
@GameTestHolder(_UtilityMobs.MODID)
public class ColossusPhysicsTests {

    private static final int WATCH_TICKS = 200;
    private static final boolean TRACE = Boolean.getBoolean("utilitymobs.colossusTrace");

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void fill(GameTestHelper helper, Block block, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static EntityColossalGolem colossus(GameTestHelper helper, double x, double z) {
        return helper.spawn(ModEntities.STONE_COLOSSUS.get(), new net.minecraft.world.phys.Vec3(x, 1.0, z));
    }

    /// Tracks every colossus each tick; at WATCH_TICKS fails if any rose more than maxRise blocks above
    /// the top of the floor at any point, else succeeds.
    private static void watch(GameTestHelper helper, String label, double maxRise, List<? extends Mob> watched) {
        double floorTop = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        double[] highest = new double[watched.size()];
        java.util.Arrays.fill(highest, Double.NEGATIVE_INFINITY);
        helper.onEachTick(() -> {
            for (int i = 0; i < watched.size(); i++) {
                Mob mob = watched.get(i);
                if (mob.isAlive()) {
                    highest[i] = Math.max(highest[i], mob.getY() - floorTop);
                    if (TRACE && mob.tickCount % 4 == 0 && mob.tickCount < 120) {
                        _UtilityMobs.console(String.format("[colossus-trace] %s t=%d y=%.3f dy=%.3f ground=%s water=%s eyeWater=%s fluidAtFeet=%s nav=%s vehicle=%s",
                            label, mob.tickCount, mob.getY() - floorTop, mob.getDeltaMovement().y, mob.onGround(), mob.isInWater(),
                            mob.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get()),
                            mob.level().getFluidState(mob.blockPosition()).getType(), mob.getNavigation().getClass().getSimpleName(),
                            mob.getVehicle()));
                    }
                }
            }
        });
        helper.runAtTickTime(WATCH_TICKS, () -> {
            StringBuilder report = new StringBuilder();
            double worst = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < watched.size(); i++) {
                report.append(String.format(" #%d max rise %.3f", i, highest[i]));
                worst = Math.max(worst, highest[i]);
            }
            _UtilityMobs.console("[colossus-physics] " + label + ":" + report);
            if (worst > maxRise) {
                helper.fail(label + ": a colossus rose " + String.format("%.3f", worst) + " blocks above the floor (limit " + maxRise + ")");
            }
            helper.succeed();
        });
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossusAloneStaysDown(GameTestHelper helper) {
        floor(helper);
        watch(helper, "alone", 0.6, List.of(colossus(helper, 8.0, 8.0)));
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossiTouchingStayDown(GameTestHelper helper) {
        floor(helper);
        watch(helper, "touching", 0.6, List.of(colossus(helper, 7.0, 8.0), colossus(helper, 9.0, 8.0)));
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossiOverlappingStayDown(GameTestHelper helper) {
        floor(helper);
        watch(helper, "overlapping", 0.6, List.of(colossus(helper, 8.0, 8.0), colossus(helper, 8.3, 8.2)));
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossusCrowdStaysDown(GameTestHelper helper) {
        floor(helper);
        List<EntityColossalGolem> crowd = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            crowd.add(colossus(helper, 6.0 + (i % 3) * 1.5, 6.0 + (i / 3) * 1.5));
        }
        watch(helper, "crowd of six", 0.6, crowd);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossusWithGolemPassengerStaysDown(GameTestHelper helper) {
        floor(helper);
        EntityColossalGolem colossus = colossus(helper, 8.0, 8.0);
        EntityStoneGolem rider = helper.spawn(ModEntities.STONE_GOLEM.get(), new BlockPos(8, 1, 10));
        rider.startRiding(colossus, true);
        watch(helper, "golem passenger", 0.6, List.of(colossus));
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossusAtStepAndWallStaysDown(GameTestHelper helper) {
        floor(helper);
        // A one-block step it may climb (maxUpStep 1.0) and a three-block wall it may not.
        fill(helper, Blocks.STONE, 10, 1, 0, 15, 1, 15);
        fill(helper, Blocks.STONE, 0, 1, 0, 2, 3, 15);
        watch(helper, "step and wall", 1.6, List.of(colossus(helper, 8.0, 8.0), colossus(helper, 4.5, 8.0)));
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossusInWaterStaysDown(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 3, 1, 3, 12, 4, 3);
        fill(helper, Blocks.STONE, 3, 1, 12, 12, 4, 12);
        fill(helper, Blocks.STONE, 3, 1, 3, 3, 4, 12);
        fill(helper, Blocks.STONE, 12, 1, 3, 12, 4, 12);
        fill(helper, Blocks.WATER, 4, 1, 4, 11, 4, 11);
        watch(helper, "deep water", 0.6, List.of(colossus(helper, 8.0, 8.0)));
    }

    /// Turrets are the other entities that sink (EntityUtilityGolem.sinks), through the same override.
    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void turretInWaterStaysDown(GameTestHelper helper) {
        floor(helper);
        fill(helper, Blocks.STONE, 3, 1, 3, 12, 4, 3);
        fill(helper, Blocks.STONE, 3, 1, 12, 12, 4, 12);
        fill(helper, Blocks.STONE, 3, 1, 3, 3, 4, 12);
        fill(helper, Blocks.STONE, 12, 1, 3, 12, 4, 12);
        fill(helper, Blocks.WATER, 4, 1, 4, 11, 4, 11);
        watch(helper, "turret in deep water", 0.6, List.of(
            helper.spawn(ModEntities.STONE_TURRET.get(), new net.minecraft.world.phys.Vec3(8.0, 1.0, 8.0))));
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "arena16", timeoutTicks = WATCH_TICKS + 40)
    public static void colossusFightingStaysDown(GameTestHelper helper) {
        floor(helper);
        EntityColossalGolem colossus = colossus(helper, 8.0, 8.0);
        for (int i = 0; i < 4; i++) {
            helper.spawn(EntityType.ZOMBIE, new BlockPos(3 + i * 3, 1, 3));
        }
        watch(helper, "fighting zombies", 0.6, List.of(colossus));
    }
}
