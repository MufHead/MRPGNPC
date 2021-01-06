package com.muffinhead.MRPGNPC.Effects;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.Event;
import cn.nukkit.event.entity.*;
import cn.nukkit.level.MovingObjectPosition;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import com.muffinhead.MRPGNPC.NPCs.MobNPC;

public class Bullet extends EntityProjectile {
    int networkID = 81;
    Position spawnPosition;
    public double MaxDistance = 0;
    public float damage = 0f;
    public float knockback = 0f;
    double motionx = 0;
    double motiony = 0;
    double motionz = 0;
    double fyaw = 0;
    double fpitch = 0;
    public Entity master;
    public Bullet(FullChunk chunk, CompoundTag nbt, Entity entity, int networkID, Vector3 motion, double yaw, double pitch) {
        super(chunk, nbt);
        this.shootingEntity = entity;
        this.networkID = networkID;
        this.spawnPosition = this.getPosition();
        motionx = motion.x;
        motiony = motion.y;
        motionz = motion.z;
        fyaw = yaw;
        fpitch = pitch;
    }

    @Override
    public int getNetworkId() {
        return networkID;
    }

    @Override
    protected float getGravity() {
        return 0;
    }

    @Override
    public boolean entityBaseTick(int i) {
        if (!this.isClosed()) {
            if (this.getPosition()!=null) {
                try {
                    if (this.getPosition().distance(spawnPosition) >= MaxDistance) {
                        this.close();
                    }
                } catch (Exception e) {
                }
            }
            this.setMotion(new Vector3(motionx, motiony, motionz));
            this.setRotation(fyaw, fpitch);
        }
        return super.entityBaseTick(i);
    }
    @Override
    public void onCollideWithEntity(Entity entity) {
        ProjectileHitEvent e = new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity));
        this.server.getPluginManager().callEvent(e);
        if (!e.isCancelled()) {
            float damage = (float) this.getResultDamage();
            Object ev;
            if (this.shootingEntity == null) {
                ev = new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
            } else {
                ev = new EntityDamageByChildEntityEvent(this.shootingEntity, this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
            }

            if (entity.attack((EntityDamageEvent) ev)) {
                if (this.fireTicks > 0) {
                    EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, entity, 5);
                    this.server.getPluginManager().callEvent((Event) ev);
                    if (!event.isCancelled()) {
                        entity.setOnFire(event.getDuration());
                    }
                }
            }
            if (this.closeOnCollide) {
                this.close();
            }
        }
    }

    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        } else {
            int tickDiff = currentTick - this.lastUpdate;
            if (tickDiff <= 0 && !this.justCreated) {
                return true;
            } else {
                this.lastUpdate = currentTick;
                boolean hasUpdate = this.entityBaseTick(tickDiff);
                if (this.isAlive()) {
                    MovingObjectPosition movingObjectPosition = null;
                    if (!this.isCollided) {
                        this.motionY -= (double)this.getGravity();
                    }
                    Vector3 moveVector = new Vector3(this.x + this.motionX, this.y + this.motionY, this.z + this.motionZ);
                    Entity[] list = this.getLevel().getCollidingEntities(this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D), this);
                    double nearDistance = 2.147483647E9D;
                    Entity nearEntity = null;
                    Entity[] var10 = list;
                    int var11 = list.length;

                    for(int var12 = 0; var12 < var11; ++var12) {
                        Entity entity = var10[var12];
                        if (entity == shootingEntity){
                            entity = null;
                        }
                        if (entity != null) {
                            if (entity != this.shootingEntity || this.ticksLived >= 5) {
                                AxisAlignedBB axisalignedbb = entity.boundingBox.grow(0.3D, 0.3D, 0.3D);
                                MovingObjectPosition ob = axisalignedbb.calculateIntercept(this, moveVector);
                                if (ob != null) {
                                    double distance = this.distanceSquared(ob.hitVector);
                                    if (distance < nearDistance) {
                                        nearDistance = distance;
                                        nearEntity = entity;
                                    }
                                }
                            }
                        }
                    }

                    if (nearEntity != null) {
                        movingObjectPosition = MovingObjectPosition.fromEntity(nearEntity);
                    }

                    if (movingObjectPosition != null && movingObjectPosition.entityHit != null) {
                        this.onCollideWithEntity(movingObjectPosition.entityHit);
                        return true;
                    }

                    this.move(this.motionX, this.motionY, this.motionZ);

                    if (!this.hadCollision || Math.abs(this.motionX) > 1.0E-5D || Math.abs(this.motionY) > 1.0E-5D || Math.abs(this.motionZ) > 1.0E-5D) {
                        double f = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
                        this.yaw = Math.atan2(this.motionX, this.motionZ) * 180.0D / 3.141592653589793D;
                        this.pitch = Math.atan2(this.motionY, f) * 180.0D / 3.141592653589793D;
                        hasUpdate = true;
                    }

                    this.updateMovement();
                }

                return hasUpdate;
            }
        }
    }
}
