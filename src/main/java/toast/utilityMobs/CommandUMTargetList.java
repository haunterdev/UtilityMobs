package toast.utilityMobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The shared implementation behind /umwhitelist and /umblacklist.
 *
 * <p>Both commands are the same tool over a different config list, so 1.12.2's two near-identical
 * classes are one builder here, parameterised by the option they edit. The syntax, messages and
 * behaviour are unchanged:
 *
 * <pre>/um&lt;list&gt; add|remove [entityId] | list | clear</pre>
 *
 * <p>add/remove with no entity argument act on whatever entity the player is looking at; with an
 * argument they take an entity registry id. Both edit the config and live-reload, so a change takes
 * effect immediately and stays in sync with the config screen.
 *
 * <p>The id argument is a greedyString, not StringArgumentType.string(). Brigadier's unquoted string
 * reader only accepts [a-zA-Z0-9_.+-], and a colon is not in that set, so "string" could not parse a
 * namespaced id at all: typing minecraft:cow failed at the colon, and suggestion filtering died there
 * too, which is why ids like minecraft:husk looked missing. greedyString takes the rest of the line
 * verbatim, which is safe here because the id is always the last argument.
 */
public final class CommandUMTargetList {
    private CommandUMTargetList() {}

    private static final double LOOK_RANGE = 16.0;

    public static LiteralArgumentBuilder<CommandSourceStack> whitelist() {
        return CommandUMTargetList.build("umwhitelist", "attack_whitelist", "global attack whitelist");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> blacklist() {
        return CommandUMTargetList.build("umblacklist", "attack_blacklist", "global attack blacklist");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name, String configField, String label) {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("list")
                .executes(ctx -> CommandUMTargetList.list(ctx.getSource(), configField, label)))
            .then(Commands.literal("clear")
                .executes(ctx -> CommandUMTargetList.clear(ctx.getSource(), configField, label)))
            .then(Commands.literal("add")
                .executes(ctx -> CommandUMTargetList.edit(ctx.getSource(), configField, label, null, true))
                .then(Commands.argument("entityId", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CommandUMTargetList.entityIds(), builder))
                    .executes(ctx -> CommandUMTargetList.edit(ctx.getSource(), configField, label,
                        StringArgumentType.getString(ctx, "entityId"), true))))
            .then(Commands.literal("remove")
                .executes(ctx -> CommandUMTargetList.edit(ctx.getSource(), configField, label, null, false))
                .then(Commands.argument("entityId", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CommandUMTargetList.entityIds(), builder))
                    .executes(ctx -> CommandUMTargetList.edit(ctx.getSource(), configField, label,
                        StringArgumentType.getString(ctx, "entityId"), false))));
    }

    private static int list(CommandSourceStack source, String configField, String label) {
        List<String> entries = CommandUMTargetList.entries(configField);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("The " + label + " is empty."), false);
        }
        else {
            source.sendSuccess(() -> Component.literal("The " + label + " (" + entries.size() + "): " + String.join(", ", entries)), false);
        }
        return entries.size();
    }

    private static int clear(CommandSourceStack source, String configField, String label) {
        Properties.setList(Properties.GENERAL, configField, new ArrayList<String>());
        source.sendSuccess(() -> Component.literal("Cleared the " + label + "."), true);
        return 1;
    }

    private static int edit(CommandSourceStack source, String configField, String label, @Nullable String idArg, boolean add) throws CommandSyntaxException {
        List<String> entries = CommandUMTargetList.entries(configField);
        String id = CommandUMTargetList.resolveId(source, idArg);

        if (add) {
            if (entries.contains(id)) {
                source.sendSuccess(() -> Component.literal(id + " is already on the " + label + "."), false);
                return 0;
            }
            entries.add(id);
            Properties.setList(Properties.GENERAL, configField, entries);
            source.sendSuccess(() -> Component.literal("Added " + id + " to the " + label + "."), true);
        }
        else {
            if (!entries.remove(id)) {
                source.sendSuccess(() -> Component.literal(id + " was not on the " + label + "."), false);
                return 0;
            }
            Properties.setList(Properties.GENERAL, configField, entries);
            source.sendSuccess(() -> Component.literal("Removed " + id + " from the " + label + "."), true);
        }
        return 1;
    }

    /// The explicit argument if given, otherwise the id of the entity the sender is looking at.
    private static String resolveId(CommandSourceStack source, @Nullable String idArg) throws CommandSyntaxException {
        if (idArg != null) {
            // Accept any namespaced registry id even if it doesn't resolve right now - the list matches by
            // id at runtime, so a valid modded id must not be rejected here.
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(idArg))
                    && idArg.indexOf(':') < 0 && !"Player".equals(idArg) && !"Hostiles".equals(idArg)) {
                throw new SimpleCommandExceptionType(Component.literal(
                    "Unknown entity id: " + idArg + " (use e.g. minecraft:cow, or look at a mob and omit the id).")).create();
            }
            return idArg;
        }
        Entity target = CommandUMTargetList.lookedAtEntity(source);
        if (target == null) {
            throw new SimpleCommandExceptionType(Component.literal(
                "Not looking at any entity. Aim at a mob, or pass an entity id (e.g. minecraft:cow).")).create();
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) {
            throw new SimpleCommandExceptionType(Component.literal(
                "That entity has no registry id and can't be listed by looking at it.")).create();
        }
        return key.toString();
    }

    /// Finds the entity the sender (must be a player) is looking at, within LOOK_RANGE. 1.12.2 walked
    /// the nearby entities and intersected their boxes by hand; ProjectileUtil does exactly that now.
    @Nullable
    private static Entity lookedAtEntity(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            return null;
        }
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * CommandUMTargetList.LOOK_RANGE, look.y * CommandUMTargetList.LOOK_RANGE, look.z * CommandUMTargetList.LOOK_RANGE);
        AABB search = player.getBoundingBox()
            .expandTowards(look.x * CommandUMTargetList.LOOK_RANGE, look.y * CommandUMTargetList.LOOK_RANGE, look.z * CommandUMTargetList.LOOK_RANGE)
            .inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
            EntitySelector.NO_SPECTATORS, CommandUMTargetList.LOOK_RANGE * CommandUMTargetList.LOOK_RANGE);
        return hit == null ? null : hit.getEntity();
    }

    private static List<String> entries(String configField) {
        Object value = Properties.getProperty(Properties.GENERAL, configField);
        List<String> entries = new ArrayList<String>();
        if (value instanceof List<?> raw) {
            for (Object entry : raw) {
                entries.add(String.valueOf(entry));
            }
        }
        return entries;
    }

    private static List<String> entityIds() {
        List<String> ids = new ArrayList<String>();
        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            ids.add(key.toString());
        }
        Collections.sort(ids);
        return ids;
    }
}
