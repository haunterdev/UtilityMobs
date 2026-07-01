package toast.utilityMobs;

import toast.utilityMobs.block.EntityJukeboxGolem;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.server.FMLServerHandler;

public class CommonProxy
{
    // Returns the username of the player if this is the client side.
    public String getPlayer() {
        return null;
    }

    // Registers render files if this is the client side.
    public void registerRenderers() {
        // Client method
    }

    // Registers the tab-icon item's model. Client-only; called from the item registry event
    // (after the item exists - preInit runs before Register<Item> fires).
    public void registerTabIconModel() {
        // Client method
    }

    // Plays a record for the jukebox golem.
    public void playRecordGolem(EntityJukeboxGolem golem, String record) {
        // Client method
    }

    // Spawns a floating heal number above an entity. Client-only; server calls reach clients via entity status.
    public void spawnHealNumber(Entity entity, float amount) {
        // Client method
    }

    // Called at the end of each client tick.
    public void handleClientTick() {
        // Client method
    }

    // Returns true if entities are allowed to block movement. Namely, if they can be stood on.
    public boolean solidEntities() {
        return !FMLServerHandler.instance().getServer().isDedicatedServer();
    }
}
