package com.danielgamer321.rotp_sf.entity.damaging.projectile.ownerbound;

import java.util.UUID;

import com.danielgamer321.rotp_sf.entity.stand.stands.StoneFreeEntity;
import com.danielgamer321.rotp_sf.init.InitEntities;
import com.danielgamer321.rotp_sf.init.InitSounds;
import com.danielgamer321.rotp_sf.init.InitStands;
import com.github.standobyte.jojo.entity.damaging.projectile.ownerbound.OwnerBoundProjectileEntity;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mod.JojoModUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class SFGrapplingStringEntity extends OwnerBoundProjectileEntity {
	private static final UUID MANUAL_MOVEMENT_LOCK = UUID.fromString("ccf94bd5-8f0f-4d1e-b606-ba0773d963f3");
    private IStandPower userStandPower;
    private boolean bindEntities;
    private StandEntity stand;
    private boolean placedBarrier = false;
    private boolean caughtAnEntity = false;

    public SFGrapplingStringEntity(World world, StandEntity entity, IStandPower userStand) {
        super(InitEntities.SF_GRAPPLING_STRING.get(), entity, world);
        this.stand = entity;
        this.userStandPower = userStand;
    }
    
    public SFGrapplingStringEntity(EntityType<? extends SFGrapplingStringEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    public void remove() {
    	super.remove();
    	if (!level.isClientSide() && stand != null && caughtAnEntity) {
    		stand.getManualMovementLocks().removeLock(MANUAL_MOVEMENT_LOCK);
    	}
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) {
            return;
        }
        if (!level.isClientSide() && (userStandPower == null || userStandPower.getHeldAction() != (
        		bindEntities ? InitStands.STONE_FREE_GRAPPLE_ENTITY.get() : 
        		    InitStands.STONE_FREE_GRAPPLE.get()))) {
        	remove();
        	return;
        }
        LivingEntity bound = getEntityAttachedTo();
        if (bound != null) {
            LivingEntity owner = getOwner();
            if (!bound.isAlive()) {
                if (!level.isClientSide()) {
                    remove();
                }
            }
            else {
                Vector3d vecToOwner = owner.position().subtract(bound.position());
                double length = vecToOwner.length();
                if (length < 2) {
                	if (!level.isClientSide()) {
                		remove();
                	}
                }
                else {
                	dragTarget(bound, vecToOwner.normalize().scale(1));
                	bound.fallDistance = 0;
                }
            }
        }
    }
    
    public void setBindEntities(boolean bindEntities) {
        this.bindEntities = bindEntities;
    }
    
    @Override
    protected boolean moveToBlockAttached() {   
        if (super.moveToBlockAttached()) {
            LivingEntity owner = getOwner();
            Vector3d vecFromOwner = position().subtract(owner.position());
            if (vecFromOwner.lengthSqr() > 4) {
                Vector3d grappleVec = vecFromOwner.normalize().scale(2);
                Entity entity = owner;
                if (stand == null && owner instanceof StandEntity) {
                	stand = (StandEntity) owner;
                }
                if (stand != null && stand.isFollowingUser()) {
                    LivingEntity user = stand.getUser();
                    if (user != null) {
                    	entity = user;
                    }
                }
                entity = entity.getRootVehicle();
                entity.setDeltaMovement(grappleVec);
                entity.fallDistance = 0;
            }
            else if (!level.isClientSide()) {
            	remove();
            }
            return true;
        }
        return false;
    }
    
    @Override
	public boolean isBodyPart() {
        return true;
    }

    private static final Vector3d OFFSET = new Vector3d(-0.3, -0.2, 0.75);
    @Override
    protected Vector3d getOwnerRelativeOffset() {
        return OFFSET;
    }

    @Override
	public int ticksLifespan() {
        return getEntityAttachedTo() == null && !getBlockPosAttachedTo().isPresent() ? 40 : Integer.MAX_VALUE;
    }
    
    @Override
    protected float movementSpeed() {
        return 4F;
    }
    
    @Override
    protected boolean canHitEntity(Entity entity) {
    	LivingEntity owner = getOwner();
    	if (entity.is(owner) || !(entity instanceof LivingEntity)) {
    		return false;
    	}
    	if (owner instanceof StandEntity) {
    		StandEntity stand = (StandEntity) getOwner();
    		return !entity.is(stand.getUser()) || !stand.isFollowingUser();
    	}
        return true;
    }
    
    @Override
    protected boolean hurtTarget(Entity target, LivingEntity owner) {
        if (getEntityAttachedTo() == null && bindEntities) {
            if (target instanceof LivingEntity) {
                LivingEntity livingTarget = (LivingEntity) target;
                if (!JojoModUtil.isTargetBlocking(livingTarget)) {
                    attachToEntity((LivingEntity) target);
                    playSound(InitSounds.STONE_FREE_GRAPPLE_CATCH.get(), 1.0F, 1.0F);
            	    caughtAnEntity = true;
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    protected void updateMotionFlags() {}
    
    @Override
    protected void afterBlockHit(BlockRayTraceResult blockRayTraceResult, boolean brokenBlock) {
        if (!brokenBlock && !bindEntities) {
            if (!getBlockPosAttachedTo().isPresent()) {
                playSound(InitSounds.STONE_FREE_GRAPPLE_CATCH.get(), 1.0F, 1.0F);
                attachToBlockPos(blockRayTraceResult.getBlockPos());
            }
            
            placeBarrier();
        }
    }
    
    private void placeBarrier() {

        if (!level.isClientSide() && !placedBarrier && getOwner() instanceof StoneFreeEntity) {
            StoneFreeEntity stonefree = (StoneFreeEntity) getOwner();
            if (stonefree.hasBarrierAttached()) {
                stonefree.attachBarrier(blockPosition());
            }
            placedBarrier = true;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public float getBaseDamage() {
        return 0;
    }

    @Override
    protected float getMaxHardnessBreakable() {
        return 0;
    }

    @Override
    public boolean standDamage() {
        return true;
    }
}
