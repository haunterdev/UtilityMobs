package toast.utilityMobs.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import toast.utilityMobs.block.EntityJukeboxGolem;

/// A record that follows its jukebox golem around. 1.12.2's MovingSound is AbstractTickableSoundInstance here.
public class MovingSoundRecord extends AbstractTickableSoundInstance {

    private final EntityJukeboxGolem golem;
    private final String record;

    public MovingSoundRecord(EntityJukeboxGolem golem, String record, SoundEvent sound) {
        super(sound, SoundSource.RECORDS, RandomSource.create());
        // Standard volume + LINEAR attenuation. The original 4.0 pushed the max-gain radius out to ~64
        // blocks, so when you stood near the golem OpenAL couldn't spatialize the source and the music
        // seemed to come from a random direction. 1.0 keeps directionality correct at jukebox range.
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.looping = false;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;

        this.golem = golem;
        this.x = golem.getX();
        this.y = golem.getY() + golem.getBbHeight() * 0.5; // emit from the body, not the feet
        this.z = golem.getZ();

        this.record = record;
    }

    @Override
    public void tick() {
        if (!this.golem.isAlive() || !this.golem.getRecord().equals(this.record)) {
            this.stop();
        }
        else {
            this.x = this.golem.getX();
            this.y = this.golem.getY() + this.golem.getBbHeight() * 0.5;
            this.z = this.golem.getZ();
        }
    }
}
