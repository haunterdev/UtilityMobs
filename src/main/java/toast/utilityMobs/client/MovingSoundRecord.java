package toast.utilityMobs.client;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs.block.EntityJukeboxGolem;

@SideOnly(Side.CLIENT)
public class MovingSoundRecord extends MovingSound {

    private final EntityJukeboxGolem golem;
    private final String record;

    public MovingSoundRecord(EntityJukeboxGolem golem, String record, SoundEvent sound) {
        super(sound, SoundCategory.RECORDS);
        // Standard volume + LINEAR attenuation. The original 4.0 pushed the max-gain radius out to ~64
        // blocks, so when you stood near the golem OpenAL couldn't spatialize the source and the music
        // seemed to come from a random direction. 1.0 keeps directionality correct at jukebox range.
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.repeat = false;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.LINEAR;

        this.golem = golem;
        this.xPosF = (float)golem.posX;
        this.yPosF = (float)(golem.posY + golem.height * 0.5D); // emit from the body, not the feet
        this.zPosF = (float)golem.posZ;

        this.record = record;
    }

    @Override
    public void update() {
        if (this.golem.isDead || !this.golem.getRecord().equals(this.record)) {
            this.donePlaying = true;
        }
        else {
            this.xPosF = (float)this.golem.posX;
            this.yPosF = (float)(this.golem.posY + this.golem.height * 0.5D);
            this.zPosF = (float)this.golem.posZ;
        }
    }
}
