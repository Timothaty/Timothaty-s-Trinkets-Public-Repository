package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class VFXIndulgencyBlessingEntity extends Mob {

	public static final int LIFETIME_TICKS = 20;

	public VFXIndulgencyBlessingEntity(EntityType<VFXIndulgencyBlessingEntity> type, Level world) {
		super(type, world);
		xpReward = 0;

		setNoAi(true);
		setSilent(true);
		setPersistenceRequired();
		setNoGravity(true);
		this.noPhysics = true;
	}

	@Override
	protected void registerGoals() {
	}

	@Override
	public void tick() {
		super.tick();

		setDeltaMovement(Vec3.ZERO);
		setNoGravity(true);

		if (!level().isClientSide() && tickCount >= LIFETIME_TICKS) {
			discard();
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entityIn) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0);
		builder = builder.add(Attributes.MAX_HEALTH, 1);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 0);
		builder = builder.add(Attributes.STEP_HEIGHT, 0);
		return builder;
	}
}