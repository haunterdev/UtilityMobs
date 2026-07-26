package toast.utilityMobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * /umsummon &lt;type&gt; [count] [team|hostile]
 *
 * Batch-spawns Utility Mobs golems/turrets/colossi at the sender - handy for mob battles without the
 * spawn-egg mass-spawn pitfall. With no team arg the golems are owned by the sender (friendly to them);
 * with a team arg they join battle team "team_&lt;name&gt;" and fight golems on other teams.
 *
 * <p>The 1.12.2 syntax is unchanged, so the type list is still the lowercased UTILITY_NAMES rather than
 * a generic entity-type argument.
 */
public final class CommandUMSummon {
    private CommandUMSummon() {}

    private static final int MAX_COUNT = 500;
    private static final List<String> TYPES = CommandUMSummon.buildTypeList();

    private static final SimpleCommandExceptionType NOT_A_GOLEM =
        new SimpleCommandExceptionType(Component.literal("That is not a summonable Utility Mobs golem."));

    private static List<String> buildTypeList() {
        List<String> list = new ArrayList<String>();
        list.add("umirongolem");
        list.add("umsnowgolem");
        for (String[] group : _UtilityMobs.UTILITY_NAMES) {
            for (String name : group) {
                list.add(name.toLowerCase(Locale.ROOT));
            }
        }
        Collections.sort(list);
        return list;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("umsummon")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CommandUMSummon.TYPES, builder))
                .executes(ctx -> CommandUMSummon.summon(ctx.getSource(), StringArgumentType.getString(ctx, "type"), 1, null))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, CommandUMSummon.MAX_COUNT))
                    .executes(ctx -> CommandUMSummon.summon(ctx.getSource(),
                        StringArgumentType.getString(ctx, "type"),
                        IntegerArgumentType.getInteger(ctx, "count"), null))
                    .then(Commands.argument("team", StringArgumentType.word())
                        .executes(ctx -> CommandUMSummon.summon(ctx.getSource(),
                            StringArgumentType.getString(ctx, "type"),
                            IntegerArgumentType.getInteger(ctx, "count"),
                            StringArgumentType.getString(ctx, "team"))))));
    }

    /// teamArg is the literal third argument: either "hostile" or a battle team name, or null for neither.
    private static int summon(CommandSourceStack source, String type, int count, String teamArg) throws CommandSyntaxException {
        type = type.toLowerCase(Locale.ROOT);
        EntityType<?> entityType = CommandUMSummon.lookUp(type);
        if (entityType == null) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown Utility Mobs type: " + type)).create();
        }
        boolean hostile = "hostile".equalsIgnoreCase(teamArg);
        String team = teamArg != null && !hostile ? EntityUtilityGolem.TEAM_PREFIX + teamArg.toLowerCase(Locale.ROOT) : null;

        ServerLevel level = source.getLevel();
        Entity senderEnt = source.getEntity();
        Vec3 at = source.getPosition();
        double cx = Math.floor(at.x) + 0.5, cy = Math.floor(at.y), cz = Math.floor(at.z) + 0.5;
        String owner = team != null ? team : hostile ? null
            : senderEnt instanceof Player player ? player.getGameProfile().getName() : null;
        // Spread the spawns so they don't stack in one block; wider for bigger batches.
        double spread = Math.min(10.0, 1.0 + count * 0.15);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = entityType.create(level);
            if (!(entity instanceof EntityUtilityGolem golem)) {
                throw CommandUMSummon.NOT_A_GOLEM.create();
            }
            double ox = (level.random.nextDouble() - 0.5) * spread;
            double oz = (level.random.nextDouble() - 0.5) * spread;
            golem.moveTo(cx + ox, cy, cz + oz, level.random.nextFloat() * 360.0F, 0.0F);
            golem.setOwner(owner);
            golem.setAggressive(hostile);
            golem.finalizeSpawn(level, level.getCurrentDifficultyAt(golem.blockPosition()), MobSpawnType.COMMAND, null, null);
            if (level.addFreshEntity(golem)) {
                spawned++;
            }
        }
        final int total = spawned;
        final String typeName = type;
        final String teamName = team;
        source.sendSuccess(() -> Component.literal("Summoned " + total + " " + typeName + (teamName != null ? " on " + teamName : "")), true);
        return spawned;
    }

    /// Finds the entity type for a 1.12.2 command type name.
    ///
    /// The command syntax is unchanged, so it still takes the lowercased class name (chestendergolem),
    /// but 1.20.1 registry ids are snake_case (chest_ender_golem) and a few are neither
    /// (umirongolem, armorcolossus). Rather than keep a hand-written table in step with ModEntities,
    /// the mod's own ids are matched with their underscores removed, which collapses every spelling.
    private static EntityType<?> lookUp(String type) {
        for (EntityType<?> candidate : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(candidate);
            if (key != null && _UtilityMobs.MODID.equals(key.getNamespace())
                    && key.getPath().replace("_", "").equals(type)) {
                return candidate;
            }
        }
        return null;
    }
}
