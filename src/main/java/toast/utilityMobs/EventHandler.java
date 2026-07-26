package toast.utilityMobs;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import toast.utilityMobs.colossal.EntityColossalGolem;
import toast.utilityMobs.golem.EntityUtilityGolem;
import toast.utilityMobs.turret.EntityTurretGolem;

/**
 * The mod's Forge event subscribers.
 *
 * <p>1.12.2's onConfigChanged is gone: it existed to re-read the file after the in-game config GUI
 * saved it, and ForgeConfigSpec now fires its own reload event, which Properties already listens for.
 */
public class EventHandler
{
    public EventHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Gives ownership of an unowned utility golem to the nearest player when it enters the world - so a
     * golem spawned from a creative spawn egg becomes YOURS (commandable, friendly) instead of ownerless.
     * Golems built in-world (BuildHelper) already call setOwner() before spawning, so they are skipped;
     * saved golems restore their owner from NBT before this fires, so reloads are not reassigned.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide || !(entity instanceof EntityUtilityGolem golem))
            return;
        String owner = golem.getOwnerName();
        if (owner != null && !owner.isEmpty())
            return; // already owned (built in-world, or restored from save, or a /umsummon team golem)
        Player player = entity.level().getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), 8.0, false);
        if (player != null) {
            golem.setOwner(player.getGameProfile().getName());
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
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof EntityUtilityGolem golem))
            return;
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        // 1.12.2 had one Items.DYE whose metadata picked the colour; each dye is its own item now.
        if (held.isEmpty() || !(held.getItem() instanceof DyeItem dye))
            return;
        if (!player.getAbilities().instabuild && !golem.canInteract(player))
            return;
        if (!event.getLevel().isClientSide) {
            String color = dye.getDyeColor().getName();
            golem.setOwner(EntityUtilityGolem.TEAM_PREFIX + color);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.literal("Team: " + color), true);
        }
        event.setCanceled(true);
    }

    /**
     * Called by LivingEntity.die() - adds skeleton/creeper skull drops.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDrops(LivingDropsEvent event) {
        // Wither skeletons are a separate class, so Skeleton is only the normal skeleton.
        int skullRarity = Properties.getInt(Properties.GENERAL, "skull_rarity");
        int creeperRarity = Properties.getInt(Properties.GENERAL, "creeper_head_rarity");
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.level().isClientSide || !event.isRecentlyHit())
            return;
        if (skullRarity > 0 && dead instanceof Skeleton) {
            int rarity = skullRarity - event.getLootingLevel();
            if (rarity <= 0 || dead.getRandom().nextInt(rarity) == 0) {
                EventHandler.addDrop(event, dead, new ItemStack(Items.SKELETON_SKULL));
            }
        }
        else if (creeperRarity > 0 && dead instanceof Creeper creeper) {
            int rarity = creeperRarity - event.getLootingLevel();
            if (creeper.isPowered()) {
                rarity >>= 1;
            }
            if (rarity <= 0 || dead.getRandom().nextInt(rarity) == 0) {
                EventHandler.addDrop(event, dead, new ItemStack(Items.CREEPER_HEAD));
            }
        }
    }

    private static void addDrop(LivingDropsEvent event, LivingEntity dead, ItemStack stack) {
        ItemEntity drop = new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), stack);
        drop.setPickUpDelay(10);
        event.getDrops().add(drop);
    }

    /**
     * Called by LivingEntity.hurt() - applies projectile upgrade effects.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource() == null)
            return;
        // Never react to explosion damage. 1.12.2 got this for free: DamageSource.causeExplosionDamage
        // built a plain DamageSource("explosion") whenever the exploder was not a living entity, so
        // getImmediateSource() was null and the "attacker is an arrow" test below failed. 1.20.1's
        // DamageSources.explosion reports the exploder itself as the DIRECT entity, so for an explosive
        // arrow the direct entity is that same arrow: the handler would fire again on the explosion it
        // had just caused, explode again, and recurse until the server died. The upgrades are meant to
        // trigger on the projectile hit, not on the blast's own damage.
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION))
            return;
        Entity attacker = event.getSource().getDirectEntity();
        LivingEntity victim = event.getEntity();
        if (!(attacker instanceof AbstractArrow) && !(attacker instanceof Projectile) && !(attacker instanceof Fireball))
            return;

        if (TargetHelper.hasOwner(attacker)) {
            TargetHelper targetHelper = TargetHelper.getOwnerTargetHelper(attacker);
            if (!targetHelper.isValidTarget(victim)) {
                event.setCanceled(true);
                return;
            }
            // Owned turret/golem fire ignores the target's hit-immunity frames, so every arrow
            // that connects deals its damage instead of bouncing off a mob that is still flashing
            // from a prior hit or is a slime fresh off a bounce. This event fires at the start of
            // hurt (before the invulnerability check), so zeroing it here lands the hit.
            victim.invulnerableTime = 0;
        }

        if (EnumUpgrade.MULTISHOT.isApplied(attacker)) {
            victim.invulnerableTime = 0;
        }
        if (EnumUpgrade.POISON.isApplied(attacker)) {
            EffectHelper.stackEffect(victim, MobEffects.POISON, 3 * 20, 0, 1);
        }
        if (EnumUpgrade.SLOW.isApplied(attacker)) {
            EffectHelper.stackEffect(victim, MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 0, 4);
        }
        if (EnumUpgrade.EXPLOSIVE.isApplied(attacker)) {
            EffectHelper.explodeSafe(attacker, 1.0F);
        }
        if (EnumUpgrade.FIRE_EXPLOSIVE.isApplied(attacker)) {
            EffectHelper.explodeFireSafe(attacker, 1.0F);
        }
        if (EnumUpgrade.EGG.isApplied(attacker)) {
            // canChangeDimensions is 1.20.1's stand-in for 1.12.2's isNonBoss: the dragon and the wither
            // are the two entities that override it to false. It is also false while riding or ridden,
            // which only means a mounted mob is never turned into a chicken.
            if (!victim.level().isClientSide && attacker instanceof AbstractArrow arrow && !(victim instanceof Player)
                    && victim.canChangeDimensions() && victim.getHealth() < arrow.getBaseDamage() * 2) {
                Level level = victim.level();
                Chicken chicken = EntityType.CHICKEN.create(level);
                if (chicken != null) {
                    chicken.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
                    level.addFreshEntity(chicken);
                }
                victim.discard();
            }
        }
    }

    /**
     * While a player rides a colossus, suppress the player's own (puny, and i-frame-stealing) melee swing so
     * the colossus's heavy punch is what actually lands. The colossus attack is driven by the attack-key packet.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerAttackWhileRiding(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player != null && player.getVehicle() instanceof EntityColossalGolem) {
            event.setCanceled(true);
        }
    }

    /**
     * While a player rides a colossus, the colossus soaks all incoming damage instead of the rider - unless the
     * source is the colossus itself. The redirected hit cannot recurse because the colossus is not a player rider.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRiderHurt(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Entity mount = event.getEntity().getVehicle();
        if (!(mount instanceof EntityColossalGolem))
            return;
        DamageSource source = event.getSource();
        if (source == null || source.getEntity() == mount || source.getDirectEntity() == mount)
            return;
        event.setCanceled(true);
        mount.hurt(source, event.getAmount());
    }

    /**
     * Called when any living entity acquires an attack target. When the no_mob_aggro option is on,
     * refuses the target (and clears the revenge target) if it is a turret, so mobs never retaliate
     * against turrets.
     *
     * <p>1.12.2's LivingSetAttackTargetEvent fired after the fact, so the only cure was to set the
     * target back to null. 1.20.1's LivingChangeTargetEvent fires first and is cancelable, so the
     * target is simply never taken - and the old comment about the null re-fire not looping is moot.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetAttackTarget(LivingChangeTargetEvent event) {
        if (!Properties.getBoolean("turrets", "no_mob_aggro"))
            return;
        if (!(event.getNewTarget() instanceof EntityTurretGolem))
            return;
        event.setCanceled(true);
        if (event.getEntity() instanceof Mob mob) {
            mob.setLastHurtByMob(null);
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
        HitResult ray = event.getRayTraceResult();
        if (!(ray instanceof EntityHitResult hit) || !(hit.getEntity() instanceof EntityUtilityGolem hitGolem))
            return;
        Projectile projectile = event.getProjectile();
        if (projectile == null)
            return;
        // 1) Shooter-based: the most reliable signal. The firing golem is a friend of the hit golem
        //    unless they are on opposing battle teams (so you can still shoot enemy /umsummon golems).
        //    1.12.2 needed obfuscation-safe reflection to read the shooter; Projectile.getOwner() is public now.
        Entity shooter = projectile.getOwner();
        if (shooter instanceof EntityUtilityGolem shooterGolem && !shooterGolem.isEnemyTeam(hitGolem)) {
            event.setCanceled(true);
            return;
        }
        // 2) Owner-tag fallback: same owner, or a golem with no owner yet (freshly built/summoned).
        if (TargetHelper.hasOwner(projectile)) {
            String projOwner = projectile.getPersistentData().getString("UM|owner");
            String golemOwner = hitGolem.getOwnerName();
            if (projOwner != null && !projOwner.isEmpty()
                    && (projOwner.equals(golemOwner) || golemOwner == null || golemOwner.isEmpty())) {
                event.setCanceled(true);
            }
        }
    }
}
