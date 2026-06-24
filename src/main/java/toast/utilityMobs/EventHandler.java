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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerAttackWhileRiding(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != null && player.getRidingEntity() instanceof EntityColossalGolem) {
            event.setCanceled(true);
        }
    }

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