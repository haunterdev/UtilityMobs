package toast.utilityMobs;

import toast.utilityMobs.block.EntityJukeboxGolem;
import toast.utilityMobs.network.MessageExplosion;
import net.minecraft.entity.Entity;

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

    // Handles an incoming heal-number packet on the client's main thread.
    // Lives on the proxy so the packet handler itself stays free of client-only classes and can be
    // registered (and therefore encoded) on a dedicated server.
    public void handleHealNumber(int entityId, float amount) {
        // Client method
    }

    // Handles an incoming explosion-effect packet on the client's main thread. Same reason as above.
    public void handleExplosionFx(MessageExplosion message) {
        // Client method
    }

    // Called at the end of each client tick.
    public void handleClientTick() {
        // Client method
    }

}
