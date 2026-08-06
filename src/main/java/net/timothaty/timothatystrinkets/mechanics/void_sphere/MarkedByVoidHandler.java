package net.timothaty.timothatystrinkets.mechanics.void_sphere;

import org.joml.Vector3f;

import javax.annotation.Nullable;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class MarkedByVoidHandler {
	private static final String NBT_OWNER_UUID = "ttr_void_mark_owner_uuid";
	private static final String NBT_STORED_DAMAGE = "ttr_void_mark_stored_damage";
	private static final String NBT_ACTIVE = "ttr_void_mark_active";
	private static final String NBT_RELEASING = "ttr_void_mark_releasing";

	private static final float DAMAGE_RELEASE_MULTIPLIER = 0.25F;
	private static final double BURST_RADIUS = 5.0D;
	private static final DustParticleOptions VOID_BURST_FLOOR_DUST = new DustParticleOptions(new Vector3f(0.72F, 0.30F, 1.0F), 0.85F);
	private static final int FLOOR_DUST_SEARCH_UP_BLOCKS = 2;
	private static final int FLOOR_DUST_SEARCH_DOWN_BLOCKS = 5;
	private static final int FLOOR_DUST_WAVE_DURATION_TICKS = 12;
	private static final double FLOOR_DUST_RING_POINTS_PER_BLOCK = 9.0D;
	private static final double FLOOR_DUST_OUTWARD_SPEED = 0.055D;
	private static final int CENTER_REVERSE_PORTAL_PARTICLES = 32;

	private static boolean applyingVoidBurstDamage = false;
	private static final List<VoidBurstFloorDustWave> activeFloorDustWaves = new ArrayList<>();


	public static void beginMark(LivingEntity target, Player owner) {
		if (target == null || owner == null)
			return;

		CompoundTag data = target.getPersistentData();
		data.putUUID(NBT_OWNER_UUID, owner.getUUID());
		data.putFloat(NBT_STORED_DAMAGE, 0.0F);
		data.putBoolean(NBT_ACTIVE, true);
		data.remove(NBT_RELEASING);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || applyingVoidBurstDamage)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || target.level().isClientSide())
			return;

		if (!hasActiveVoidMarkData(target))
			return;

		float damage = event.getNewDamage();
		if (damage <= 0.0F)
			return;

		CompoundTag data = target.getPersistentData();
		data.putFloat(NBT_STORED_DAMAGE, data.getFloat(NBT_STORED_DAMAGE) + damage);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event == null || applyingVoidBurstDamage)
			return;

		LivingEntity entity = event.getEntity();
		if (entity == null || entity.level().isClientSide())
			return;

		if (hasActiveVoidMarkData(entity)) {
			releaseMark(entity, true);
		}
	}

	@SubscribeEvent
	public static void onLevelTickPost(LevelTickEvent.Post event) {
		if (activeFloorDustWaves.isEmpty())
			return;
		if (!(event.getLevel() instanceof ServerLevel serverLevel))
			return;

		Iterator<VoidBurstFloorDustWave> iterator = activeFloorDustWaves.iterator();
		while (iterator.hasNext()) {
			VoidBurstFloorDustWave wave = iterator.next();
			if (wave.level != serverLevel)
				continue;
			if (wave.tick()) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		activeFloorDustWaves.clear();
		applyingVoidBurstDamage = false;
	}

	public static void releaseMark(LivingEntity markedEntity, boolean entityAlreadyDying) {
		if (markedEntity == null || markedEntity.level().isClientSide())
			return;

		CompoundTag data = markedEntity.getPersistentData();
		if (!hasActiveVoidMarkData(markedEntity))
			return;
		if (data.getBoolean(NBT_RELEASING))
			return;

		data.putBoolean(NBT_RELEASING, true);

		float storedDamage = data.getFloat(NBT_STORED_DAMAGE);
		UUID ownerUuid = data.hasUUID(NBT_OWNER_UUID) ? data.getUUID(NBT_OWNER_UUID) : null;

		clearVoidMarkData(markedEntity);

		float burstDamage = storedDamage * DAMAGE_RELEASE_MULTIPLIER;
		if (burstDamage <= 0.0F)
			return;

		Level level = markedEntity.level();
		Player owner = findOwner(level, ownerUuid);

		spawnVoidBurstFloorDust(level, markedEntity);
		dealVoidBurstDamage(markedEntity, owner, burstDamage, entityAlreadyDying);
	}

	private static void dealVoidBurstDamage(LivingEntity centerEntity, @Nullable Player owner, float damage, boolean centerEntityAlreadyDying) {
		Level level = centerEntity.level();

		applyingVoidBurstDamage = true;
		try {
			if (!centerEntityAlreadyDying && centerEntity.isAlive() && !isPactProtected(owner, centerEntity)) {
				hurtWithoutKnockback(centerEntity, createMagicDamageSource(centerEntity, owner), damage);
			}

			AABB area = new AABB(
					centerEntity.getX() - BURST_RADIUS,
					centerEntity.getY() - BURST_RADIUS,
					centerEntity.getZ() - BURST_RADIUS,
					centerEntity.getX() + BURST_RADIUS,
					centerEntity.getY() + BURST_RADIUS,
					centerEntity.getZ() + BURST_RADIUS
			);

			List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, living -> {
				if (living == null || !living.isAlive())
					return false;
				if (living == centerEntity)
					return false;
				if (owner != null && living == owner)
					return false;
				if (isPactProtected(owner, living))
					return false;
				return living.distanceToSqr(centerEntity) <= BURST_RADIUS * BURST_RADIUS;
			});

			for (LivingEntity victim : victims) {
				hurtWithoutKnockback(victim, createMagicDamageSource(victim, owner), damage);
			}
		} finally {
			applyingVoidBurstDamage = false;
		}
	}

	private static void hurtWithoutKnockback(LivingEntity entity, DamageSource source, float amount) {
		Vec3 movementBeforeDamage = entity.getDeltaMovement();
		int invulnerableTimeBeforeDamage = entity.invulnerableTime;

		entity.invulnerableTime = 0;
		boolean wasHurt = entity.hurt(source, amount);
		entity.invulnerableTime = Math.max(entity.invulnerableTime, invulnerableTimeBeforeDamage);

		if (wasHurt && entity.isAlive()) {
			entity.setDeltaMovement(movementBeforeDamage);
			entity.hurtMarked = false;
			entity.hasImpulse = false;
		}
	}

	private static DamageSource createMagicDamageSource(LivingEntity victim, @Nullable Player owner) {
		if (owner != null) {
			return victim.damageSources().indirectMagic(owner, owner);
		}

		return victim.damageSources().magic();
	}

	private static boolean isPactProtected(@Nullable Player owner, LivingEntity target) {
		return owner != null && PactOfAllianceHelper.areAllied(owner, target);
	}

	private static void spawnVoidBurstFloorDust(Level level, LivingEntity sourceEntity) {
		if (!(level instanceof ServerLevel serverLevel))
			return;

		VoidBurstFloorDustWave wave = new VoidBurstFloorDustWave(serverLevel, sourceEntity.getX(), sourceEntity.getY(), sourceEntity.getZ());
		wave.spawnCenterDust();
		activeFloorDustWaves.add(wave);
	}

	private static double findVoidBurstFloorY(Level level, BlockPos columnPos) {
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		int startY = columnPos.getY() + FLOOR_DUST_SEARCH_UP_BLOCKS;
		int endY = columnPos.getY() - FLOOR_DUST_SEARCH_DOWN_BLOCKS;

		for (int y = startY; y >= endY; y--) {
			mutablePos.set(columnPos.getX(), y, columnPos.getZ());

			VoxelShape floorShape = level.getBlockState(mutablePos).getCollisionShape(level, mutablePos);
			if (floorShape.isEmpty())
				continue;

			BlockPos abovePos = mutablePos.above();
			VoxelShape aboveShape = level.getBlockState(abovePos).getCollisionShape(level, abovePos);
			if (!aboveShape.isEmpty())
				continue;

			return mutablePos.getY() + floorShape.max(Direction.Axis.Y) + 0.025D;
		}

		return Double.NaN;
	}

	private static final class VoidBurstFloorDustWave {
		private final ServerLevel level;
		private final double centerX;
		private final double centerY;
		private final double centerZ;
		private int age;

		private VoidBurstFloorDustWave(ServerLevel level, double centerX, double centerY, double centerZ) {
			this.level = level;
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
		}

		private boolean tick() {
			age++;
			double progress = Math.min(1.0D, (double) age / FLOOR_DUST_WAVE_DURATION_TICKS);
			spawnRing(BURST_RADIUS * smoothStep(progress));
			return age >= FLOOR_DUST_WAVE_DURATION_TICKS;
		}

		private void spawnCenterDust() {
			double floorY = findVoidBurstFloorY(level, BlockPos.containing(centerX, centerY, centerZ));
			if (Double.isNaN(floorY))
				return;

			level.sendParticles(ParticleTypes.REVERSE_PORTAL, centerX, floorY + 0.22D, centerZ, CENTER_REVERSE_PORTAL_PARTICLES, 0.45D, 0.22D, 0.45D, 0.055D);
			level.sendParticles(VOID_BURST_FLOOR_DUST, centerX, floorY, centerZ, 8, 0.12D, 0.01D, 0.12D, 0.015D);
		}

		private void spawnRing(double radius) {
			if (radius <= 0.05D)
				return;

			int points = Math.max(10, (int) Math.ceil(radius * FLOOR_DUST_RING_POINTS_PER_BLOCK));
			double angleJitter = level.random.nextDouble() * Math.PI * 2.0D / points;

			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2.0D * i / points + angleJitter;
				double directionX = Math.cos(angle);
				double directionZ = Math.sin(angle);
				double particleX = centerX + directionX * radius;
				double particleZ = centerZ + directionZ * radius;
				double floorY = findVoidBurstFloorY(level, BlockPos.containing(particleX, centerY, particleZ));

				if (Double.isNaN(floorY))
					continue;

				level.sendParticles(VOID_BURST_FLOOR_DUST, particleX, floorY, particleZ, 0, directionX, 0.006D, directionZ, FLOOR_DUST_OUTWARD_SPEED);
			}
		}

		private static double smoothStep(double progress) {
			return progress * progress * (3.0D - 2.0D * progress);
		}
	}

	@Nullable
	private static Player findOwner(Level level, @Nullable UUID ownerUuid) {
		if (ownerUuid == null)
			return null;
		if (level instanceof ServerLevel serverLevel) {
			return serverLevel.getPlayerByUUID(ownerUuid);
		}
		return null;
	}

	private static boolean hasActiveVoidMarkData(LivingEntity entity) {
		if (entity == null)
			return false;

		CompoundTag data = entity.getPersistentData();
		return data.getBoolean(NBT_ACTIVE) && data.hasUUID(NBT_OWNER_UUID) && entity.hasEffect(TimothatysTrinketsModMobEffects.MARKED_BY_VOID);
	}

	private static void clearVoidMarkData(LivingEntity entity) {
		CompoundTag data = entity.getPersistentData();
		data.remove(NBT_OWNER_UUID);
		data.remove(NBT_STORED_DAMAGE);
		data.remove(NBT_ACTIVE);
		data.remove(NBT_RELEASING);
	}
}
