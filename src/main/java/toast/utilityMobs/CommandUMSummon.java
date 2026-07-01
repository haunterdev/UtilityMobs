package toast.utilityMobs;

import java.util.ArrayList;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import toast.utilityMobs.golem.EntityUtilityGolem;

/**
 * /umsummon &lt;type&gt; [count] [team]
 *
 * Batch-spawns Utility Mobs golems/turrets/colossi at the sender - handy for mob battles without the
 * spawn-egg mass-spawn pitfall. With no team arg the golems are owned by the sender (friendly to them);
 * with a team arg they join battle team "team_&lt;name&gt;" and fight golems on other teams.
 */
public class CommandUMSummon extends CommandBase {
    private static final int MAX_COUNT = 500;
    private static final List<String> TYPES = buildTypeList();

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

    @Override
    public String getName() {
        return "umsummon";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/umsummon <type> [count] [team|hostile] - spawns Utility Mobs golems (team = battle team, hostile = player-aggro)";
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
        String type = args[0].toLowerCase(Locale.ROOT);
        ResourceLocation rl = new ResourceLocation(_UtilityMobs.MODID, type);
        if (!EntityList.isRegistered(rl)) {
            throw new CommandException("Unknown Utility Mobs type: " + type);
        }
        int count = args.length >= 2 ? parseInt(args[1], 1, MAX_COUNT) : 1;
        boolean hostile = args.length >= 3 && args[2].equalsIgnoreCase("hostile");
        String team = (args.length >= 3 && !hostile) ? EntityUtilityGolem.TEAM_PREFIX + args[2].toLowerCase(Locale.ROOT) : null;

        World world = sender.getEntityWorld();
        Entity senderEnt = sender.getCommandSenderEntity();
        BlockPos at = sender.getPosition();
        double cx = at.getX() + 0.5, cy = at.getY(), cz = at.getZ() + 0.5;
        String owner = team != null ? team : (hostile ? null : (senderEnt instanceof EntityPlayer ? senderEnt.getName() : null));
        // Spread the spawns so they don't stack in one block; wider for bigger batches.
        double spread = Math.min(10.0, 1.0 + count * 0.15);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = EntityList.createEntityByIDFromName(rl, world);
            if (!(entity instanceof EntityUtilityGolem)) {
                throw new CommandException(type + " is not a summonable Utility Mobs golem.");
            }
            EntityUtilityGolem golem = (EntityUtilityGolem) entity;
            double ox = (world.rand.nextDouble() - 0.5) * spread;
            double oz = (world.rand.nextDouble() - 0.5) * spread;
            golem.setLocationAndAngles(cx + ox, cy, cz + oz, world.rand.nextFloat() * 360.0F, 0.0F);
            golem.setOwner(owner);
            golem.setAggressive(hostile);
            golem.onInitialSpawn(world.getDifficultyForLocation(golem.getPosition()), null);
            if (world.spawnEntity(golem)) {
                spawned++;
            }
        }
        notifyCommandListener(sender, this, "Summoned %s %s%s", spawned, type, team != null ? " on " + team : "");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, TYPES);
        }
        return Collections.emptyList();
    }
}
