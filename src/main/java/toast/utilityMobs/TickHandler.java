package toast.utilityMobs;

import java.util.ArrayDeque;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import toast.utilityMobs.event.UtilityMobsEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class TickHandler
{
    /// Stack of block events that need to be triggered.
    public static ArrayDeque<UtilityMobsEvent> eventStack = new ArrayDeque<UtilityMobsEvent>();
    /// Counter for target helper cleanup.
    private static int cleanupTicks = 0;
    /// Ticks between profiler stat dumps (200 ticks = 10s).
    private static final int PROFILE_DUMP_INTERVAL = 200;
    /// Counter for profiler stat dumps.
    private static int profileTicks = 0;

    public TickHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Called when a player logs in.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        TargetHelper.fetchTargetHelpers(event.player);
    }

    /**
     * Called when an item is crafted.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.crafting.isEmpty() || event.crafting.getItem() != Items.WRITABLE_BOOK && event.crafting.getItem() != Items.WRITTEN_BOOK || event.crafting.getTagCompound() == null || !event.crafting.getTagCompound().hasKey("umu"))
            return;
        event.crafting.getTagCompound().removeTag("umu");
        TargetHelper.read(event.player.getName(), event.crafting);
        TargetHelper.stampSignature(event.crafting);
    }

    /**
     * Saves a target book when the player finishes editing it. A book-and-quill only pushes its new
     * pages to the server stack when the GUI is closed, so polling the held book server-side and
     * reacting to a page change is effectively "save on exit", matching vanilla book behaviour.
     *
     * A "umh" signature tag tracks the last pages we wrote, so we only parse genuine player edits.
     * A target book we have never seen (e.g. one just grabbed from creative, blank pages) is filled
     * with the HOLDER'S current list instead of being parsed - parsing its blank pages would wipe
     * the holder's target list. Routing is always by the holder's name, so any player can use any
     * book; nothing is personalised.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END)
            return;
        EntityPlayer player = event.player;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getTagCompound() == null
                || held.getItem() != Items.WRITABLE_BOOK && held.getItem() != Items.WRITTEN_BOOK
                || !held.getTagCompound().hasKey("umt"))
            return;
        NBTTagCompound tag = held.getTagCompound();
        byte id = tag.getByte("umt");
        if (!tag.hasKey("umh")) {
            // Never seen this book (fresh from creative / pre-update): populate, don't parse.
            TargetHelper.write(player.getName(), held, id);
            TargetHelper.stampSignature(held);
        }
        else if (tag.getInteger("umh") != TargetHelper.signatureOf(held)) {
            // Pages changed since we last wrote them -> the player edited and closed the book.
            TargetHelper.read(player.getName(), held);
            TargetHelper.stampSignature(held);
        }
    }

    /**
     * Called each client tick.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            _UtilityMobs.proxy.handleClientTick();
        }
    }

    /**
     * Called each server tick.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Refill the per-tick golem action budgets (teleport-to-owner, wander starts).
            toast.utilityMobs.ai.EntityAIGolemFollow.resetBudget();
            toast.utilityMobs.ai.EntityAIGolemWander.resetBudget();
            if (++TickHandler.cleanupTicks > 12000) {
                TargetHelper.destroyAll();
                TickHandler.cleanupTicks = 0;
            }
            if (UMProfiler.enabled && ++TickHandler.profileTicks >= TickHandler.PROFILE_DUMP_INTERVAL) {
                UMProfiler.dump(TickHandler.profileTicks);
                TickHandler.profileTicks = 0;
            }
        }
        else if (event.phase == TickEvent.Phase.END) {
            if (!TickHandler.eventStack.isEmpty()) {
                UtilityMobsEvent modEvent;
                byte limit = 10;
                while (limit-- > 0 && (modEvent = TickHandler.eventStack.pollFirst()) != null) {
                    modEvent.execute();
                }
            }
        }
    }

    /// Puts the event into the stack.
    public static void register(UtilityMobsEvent event) {
        TickHandler.eventStack.add(event);
    }
}
