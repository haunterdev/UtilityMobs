package toast.utilityMobs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.DamageSource;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.turret.EntityTurretGolem;

public class EventHandler
{
    public EventHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Fired when the in-game config GUI saves changes. Re-reads our config values so they apply without a restart.
     */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!_UtilityMobs.MODID.equals(event.getModID())) {
            return;
        }
        if (Properties.config != null && Properties.config.hasChanged()) {
            Properties.config.save();
        }
        Properties.reload();
    }

    /**
     * Gives ownership of an unowned utility golem to the nearest player when it enters the world - so a
     * golem spawned from a creative spawn egg becomes YOURS (commandable, friendly) instead of ownerless.
     * Golems built in-world (BuildHelper) already call setOwner() before spawning, so they are skipped;
     * saved golems restore their owner from NBT before this fires, so reloads are not reassigned.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world.isRemote || !(entity instanceof EntityUtilityGolem))
            return;
        EntityUtilityGolem golem = (EntityUtilityGolem) entity;
        String owner = golem.getOwnerName();
        if (owner != null && !owner.isEmpty())
            return; // already owned (built in-world, or restored from save, or a /umsummon team golem)
        EntityPlayer player = entity.world.getClosestPlayer(entity.posX, entity.posY, entity.posZ, 8.0, false);
        if (player != null) {
            golem.setOwner(player.getName());
        }
    }

    /**
     * Right-click a utility golem with a dye to assign it to a colored battle TEAM (owner = "team_<color>").
     * Same color = allies; different colors fight each other (see EntityUtilityGolem.canAttack). Creative
     * players may team any golem; survival players only ones they can interact with. Cancels the vanilla
     * interaction so the dye isn't wasted on nothing.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDyeGolemTeam(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != EnumHand.MAIN_HAND || !(event.getTarget() instanceof EntityUtilityGolem))
            return;
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getItem() != Items.DYE)
            return;
        EntityUtilityGolem golem = (EntityUtilityGolem) event.getTarget();
        if (!player.capabilities.isCreativeMode && !golem.canInteract(player))
            return;
        if (!event.getWorld().isRemote) {
            EnumDyeColor color = EnumDyeColor.byDyeDamage(held.getMetadata() & 15);
            golem.setOwner(EntityUtilityGolem.TEAM_PREFIX + color.getName());
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            player.sendStatusMessage(new TextComponentString("Team: " + color.getName()), true);
        }
        event.setCanceled(true);
    }

    /**
     * Called by EntityLiving.onDeath() - adds skeleton/creeper skull drops.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDrops(LivingDropsEvent event) {
        // In 1.12.2 wither skeletons are a separate class, so EntitySkeleton is only the normal skeleton.
        int skullRarity = Properties.getInt(Properties.GENERAL, "skull_rarity");
        int creeperRarity = Properties.getInt(Properties.GENERAL, "creeper_head_rarity");
        if (skullRarity > 0 && event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote && event.isRecentlyHit() && event.getEntityLiving() instanceof EntitySkeleton) {
            int rarity = skullRarity - event.getLootingLevel();
            if (rarity <= 0 || event.getEntityLiving().getRNG().nextInt(rarity) == 0) {
                EntityItem drop = new EntityItem(event.getEntityLiving().world, event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ, new ItemStack(Items.SKULL));
                drop.setPickupDelay(10);
                event.getDrops().add(drop);
            }
        }
        else if (creeperRarity > 0 && event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote && event.isRecentlyHit() && event.getEntityLiving() instanceof EntityCreeper) {
            int rarity = creeperRarity - event.getLootingLevel();
            if (((EntityCreeper) event.getEntityLiving()).getPowered()) {
                rarity >>= 1;
            }
            if (rarity <= 0 || event.getEntityLiving().getRNG().nextInt(rarity) == 0) {
                EntityItem drop = new EntityItem(event.getEntityLiving().world, event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ, new ItemStack(Items.SKULL, 1, 4));
                drop.setPickupDelay(10);
                event.getDrops().add(drop);
            }
        }
    }

    /**
     * Called by EntityLivingBase.attackEntityFrom() - applies projectile upgrade effects.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource() != null) {
            Entity attacker = event.getSource().getImmediateSource();
            if (attacker instanceof EntityArrow || attacker instanceof IProjectile || attacker instanceof EntityFireball) {
                if (TargetHelper.hasOwner(attacker)) {
                    TargetHelper targetHelper = TargetHelper.getOwnerTargetHelper(attacker);
                    if (!targetHelper.isValidTarget(event.getEntityLiving())) {
                        event.setCanceled(true);
                        return;
                    }
                    // Owned turret/golem fire ignores the target's hit-immunity frames, so every arrow
                    // that connects deals its damage instead of bouncing off a mob that is still flashing
                    // from a prior hit or is a slime fresh off a bounce. This event fires at the start of
                    // attackEntityFrom (before the invulnerability check), so zeroing it here lands the hit.
                    event.getEntityLiving().hurtResistantTime = 0;
                }

                if (EnumUpgrade.MULTISHOT.isApplied(attacker)) {
                    event.getEntityLiving().hurtResistantTime = 0;
                }
                if (EnumUpgrade.POISON.isApplied(attacker)) {
                    EffectHelper.stackEffect(event.getEntityLiving(), MobEffects.POISON, 3 * 20, 0, 1);
                }
                if (EnumUpgrade.SLOW.isApplied(attacker)) {
                    EffectHelper.stackEffect(event.getEntityLiving(), MobEffects.SLOWNESS, 3 * 20, 0, 4);
                }
                if (EnumUpgrade.EXPLOSIVE.isApplied(attacker)) {
                    EffectHelper.explodeSafe(attacker, 1.0F);
                }
                if (EnumUpgrade.FIRE_EXPLOSIVE.isApplied(attacker)) {
                    EffectHelper.explodeFireSafe(attacker, 1.0F);
                }
                if (EnumUpgrade.EGG.isApplied(attacker)) {
                    if (!event.getEntityLiving().world.isRemote && attacker instanceof EntityArrow && !(event.getEntityLiving() instanceof EntityPlayer) && event.getEntityLiving().isNonBoss() && event.getEntityLiving().getHealth() < ((EntityArrow) attacker).getDamage() * 2) {
                        EntityChicken chicken = new EntityChicken(event.getEntityLiving().world);
                        chicken.setLocationAndAngles(event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ, event.getEntityLiving().rotationYaw, event.getEntityLiving().rotationPitch);
                        event.getEntityLiving().world.spawnEntity(chicken);
                        event.getEntityLiving().setDead();
                    }
                }
            }
        }
    }

    /**
     * While a player rides a colossus, suppress the player's own (puny, and i-frame-stealing) melee swing so
     * the colossus's heavy punch is what actually lands. The colossus attack is driven by the attack-key packet.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerAttackWhileRiding(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != null && player.getRidingEntity() instanceof EntityColossalGolem) {
            event.setCanceled(true);
        }
    }

    /**
     * While a player rides a colossus, the colossus soaks all incoming damage instead of the rider - unless the
     * source is the colossus itself. The redirected hit cannot recurse because the colossus is not a player rider.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRiderHurt(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer))
            return;
        Entity mount = event.getEntityLiving().getRidingEntity();
        if (!(mount instanceof EntityColossalGolem))
            return;
        DamageSource source = event.getSource();
        if (source == null || source.getTrueSource() == mount || source.getImmediateSource() == mount)
            return;
        event.setCanceled(true);
        mount.attackEntityFrom(source, event.getAmount());
    }

    /**
     * Called when any living entity acquires an attack target. When the no_mob_aggro option is on,
     * clears the target (and revenge target) if it is a turret, so mobs never retaliate against turrets.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!Properties.getBoolean("turrets", "no_mob_aggro"))
            return;
        if (!(event.getTarget() instanceof EntityTurretGolem))
            return;
        if (event.getEntityLiving() instanceof EntityLiving) {
            EntityLiving mob = (EntityLiving) event.getEntityLiving();
            // Setting null re-fires this event with a null target, which fails the instanceof check above (no loop).
            mob.setAttackTarget(null);
            mob.setRevengeTarget(null);
        }
    }

    /**
     * Called just before a projectile resolves an impact. When friendly_passthrough is on, cancels the
     * impact (so the projectile keeps flying) when it hit a FRIENDLY utility golem - i.e. one fired by a
     * turret/golem in the same army, so rows/layers of turrets shoot through each other instead of
     * plinking off the turret in front (which our friendly-fire damage cancel turns into a visible bounce).
     *
     * Two independent friendliness signals are checked, because the projectile's owner NBT tag is not
     * always present (e.g. an un-owned turret fires un-tagged arrows): the projectile's SHOOTER (the
     * firing golem, always set at construction) and the owner tag. Either one matching passes the shot.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!Properties.getBoolean("turrets", "friendly_passthrough"))
            return;
        RayTraceResult ray = event.getRayTraceResult();
        if (ray == null || ray.entityHit == null || !(ray.entityHit instanceof EntityUtilityGolem))
            return;
        EntityUtilityGolem hitGolem = (EntityUtilityGolem) ray.entityHit;
        Entity projectile = event.getEntity();
        if (projectile == null)
            return;
        // 1) Shooter-based: the most reliable signal. The firing golem is a friend of the hit golem
        //    unless they are on opposing battle teams (so you can still shoot enemy /umsummon golems).
        Entity shooter = projectileShooter(projectile);
        if (shooter instanceof EntityUtilityGolem && !((EntityUtilityGolem) shooter).isEnemyTeam(hitGolem)) {
            event.setCanceled(true);
            return;
        }
        // 2) Owner-tag fallback: same owner, or a golem with no owner yet (freshly built/summoned).
        if (TargetHelper.hasOwner(projectile)) {
            String projOwner = projectile.getEntityData().getString("UM|owner");
            String golemOwner = hitGolem.getOwnerName();
            if (projOwner != null && !projOwner.isEmpty()
                    && (projOwner.equals(golemOwner) || golemOwner == null || golemOwner.isEmpty())) {
                event.setCanceled(true);
            }
        }
    }

    /** Best-effort lookup of the entity that fired a projectile (arrow/fireball), via the obfuscation-safe
        SRG field name so it resolves in both the dev and the shipped (obfuscated) runtime. Returns null if
        the projectile type isn't handled or the field can't be read. */
    private static Entity projectileShooter(Entity projectile) {
        try {
            if (projectile instanceof EntityArrow) {
                return net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
                        EntityArrow.class, (EntityArrow) projectile, "field_70250_c");
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
