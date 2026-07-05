package toast.utilityMobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.config.Property;

/**
 * /umwhitelist add|remove [entity] | list | clear
 *
 * Admin tool for the global attack whitelist (general.attack_whitelist) - entity types golems/turrets
 * ALWAYS target even when the matching attack_* category toggle is off. Mirror of /umblacklist and edits the
 * same config option, so command and in-game config screen stay in sync. "add"/"remove" with no entity
 * argument act on whatever entity the player is looking at (point-and-whitelist); with an argument they take
 * an entity registry id (e.g. minecraft:cow). Edits the config and live-reloads, effective immediately.
 */
public class CommandUMWhitelist extends CommandBase {

    private static final double LOOK_RANGE = 16.0;

    @Override
    public String getName() {
        return "umwhitelist";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/umwhitelist <add|remove> [entityId] | list | clear - manage the global attack whitelist (no entityId = the mob you're looking at)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(this.getUsage(sender));
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        List<String> list = getWhitelist();

        if ("list".equals(sub)) {
            if (list.isEmpty()) {
                notifyCommandListener(sender, this, "Global attack whitelist is empty.");
            }
            else {
                notifyCommandListener(sender, this, "Global attack whitelist (%s): %s", list.size(), String.join(", ", list));
            }
            return;
        }
        if ("clear".equals(sub)) {
            setWhitelist(new ArrayList<String>());
            notifyCommandListener(sender, this, "Cleared the global attack whitelist.");
            return;
        }
        if (!"add".equals(sub) && !"remove".equals(sub)) {
            throw new WrongUsageException(this.getUsage(sender));
        }

        // Resolve the entity id: explicit argument, or the mob the sender is looking at.
        String id;
        if (args.length >= 2) {
            id = args[1];
            // Accept any namespaced registry id (contains ':') even if it doesn't resolve to a class right
            // now - the whitelist matches by id at runtime, so a valid modded id must not be rejected here.
            if (EntityList.getClass(new ResourceLocation(id)) == null && id.indexOf(':') < 0 && !"Player".equals(id) && !"Hostiles".equals(id)) {
                throw new CommandException("Unknown entity id: " + id + " (use e.g. minecraft:cow, or look at a mob and omit the id).");
            }
        }
        else {
            Entity target = lookedAtEntity(sender);
            if (target == null) {
                throw new CommandException("Not looking at any entity. Aim at a mob, or pass an entity id (e.g. minecraft:cow).");
            }
            ResourceLocation key = EntityList.getKey(target);
            if (key == null) {
                throw new CommandException("That entity has no registry id and can't be whitelisted by looking at it.");
            }
            id = key.toString();
        }

        if ("add".equals(sub)) {
            if (list.contains(id)) {
                notifyCommandListener(sender, this, "%s is already on the global attack whitelist.", id);
                return;
            }
            list.add(id);
            setWhitelist(list);
            notifyCommandListener(sender, this, "Added %s to the global attack whitelist.", id);
        }
        else {
            if (!list.remove(id)) {
                notifyCommandListener(sender, this, "%s was not on the global attack whitelist.", id);
                return;
            }
            setWhitelist(list);
            notifyCommandListener(sender, this, "Removed %s from the global attack whitelist.", id);
        }
    }

    // Reads the current whitelist entries from the live config.
    private static List<String> getWhitelist() {
        Property prop = Properties.config.get(Properties.GENERAL, "attack_whitelist", new String[0]);
        return new ArrayList<String>(Arrays.asList(prop.getStringList()));
    }

    // Writes the whitelist back to config, saves, and live-reloads so TargetHelper picks it up at once.
    private static void setWhitelist(List<String> entries) {
        Property prop = Properties.config.get(Properties.GENERAL, "attack_whitelist", new String[0]);
        prop.set(entries.toArray(new String[entries.size()]));
        Properties.config.save();
        Properties.reload();
    }

    // Finds the entity the sender (must be a player) is looking at, within LOOK_RANGE.
    @Nullable
    private static Entity lookedAtEntity(ICommandSender sender) {
        Entity ent = sender.getCommandSenderEntity();
        if (!(ent instanceof EntityPlayer)) {
            return null;
        }
        EntityPlayer player = (EntityPlayer) ent;
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = eye.add(look.x * LOOK_RANGE, look.y * LOOK_RANGE, look.z * LOOK_RANGE);
        AxisAlignedBB search = player.getEntityBoundingBox().expand(look.x * LOOK_RANGE, look.y * LOOK_RANGE, look.z * LOOK_RANGE).grow(1.0);
        Entity best = null;
        double bestDist = LOOK_RANGE;
        for (Entity e : player.world.getEntitiesInAABBexcluding(player, search, EntitySelectors.NOT_SPECTATING)) {
            AxisAlignedBB bb = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
            if (bb.contains(eye)) {
                return e;
            }
            RayTraceResult hit = bb.calculateIntercept(eye, end);
            if (hit != null) {
                double dist = eye.distanceTo(hit.hitVec);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }
        return best;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "remove", "list", "clear");
        }
        if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            List<String> ids = new ArrayList<String>();
            for (ResourceLocation rl : EntityList.getEntityNameList()) {
                ids.add(rl.toString());
            }
            Collections.sort(ids);
            return getListOfStringsMatchingLastWord(args, ids);
        }
        return Collections.emptyList();
    }
}
