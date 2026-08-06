package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium;

import net.timothaty.timothatystrinkets.api.damage.HolyDamageApi;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BeatificPalliumExplosionWave {
	private final Vec3 origin;
	private final float damage;
	private final long startGameTime;
	private final Set<UUID> hitTargets = new HashSet<>();
	private final Map<UUID, Boolean> pactProtection = new HashMap<>();

	public BeatificPalliumExplosionWave(Vec3 origin, float damage, long startGameTime) {
		this.origin = origin;
		this.damage = Math.max(0.0F, damage);
		this.startGameTime = startGameTime;
	}

	public boolean tick(
			ServerLevel level,
			BeatificPalliumEntity pallium,
			ServerPlayer caster,
			UUID protectedTargetUuid,
			UUID casterUuid
	) {
		long elapsed = Math.max(0L, level.getGameTime() - this.startGameTime);
		float progress = Mth.clamp((float) elapsed / BeatificPalliumData.EXPLOSION_WAVE_DURATION_TICKS, 0.0F, 1.0F);
		float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
		double radius = BeatificPalliumData.EXPLOSION_RADIUS * eased;
		if (radius > 0.0D && this.damage > 0.0F) {
			AABB search = new AABB(
					this.origin.x - radius,
					this.origin.y - BeatificPalliumData.EXPLOSION_VERTICAL_HALF_RANGE,
					this.origin.z - radius,
					this.origin.x + radius,
					this.origin.y + BeatificPalliumData.EXPLOSION_VERTICAL_HALF_RANGE,
					this.origin.z + radius
			);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, LivingEntity::isAlive)) {
				UUID targetUuid = target.getUUID();
				if (this.hitTargets.contains(targetUuid)
						|| !isValidTarget(target, protectedTargetUuid, casterUuid)
						|| !intersectsCylinder(target, radius)
						|| isPactProtected(target, caster)) {
					continue;
				}
				if (!this.hitTargets.add(targetUuid))
					continue;
				target.hurt(HolyDamageApi.indirectSource(level, pallium, caster), this.damage);
			}
		}
		return elapsed >= BeatificPalliumData.EXPLOSION_WAVE_DURATION_TICKS;
	}

	private boolean isValidTarget(LivingEntity target, UUID protectedTargetUuid, UUID casterUuid) {
		if (target instanceof ArmorStand || target.isDeadOrDying() || target.isRemoved() || target.isSpectator())
			return false;
		UUID targetUuid = target.getUUID();
		if (targetUuid.equals(protectedTargetUuid) || targetUuid.equals(casterUuid))
			return false;
		return true;
	}

	private boolean isPactProtected(LivingEntity target, ServerPlayer caster) {
		if (!(target instanceof ServerPlayer))
			return false;
		return this.pactProtection.computeIfAbsent(
				target.getUUID(),
				ignored -> caster == null || PactOfAllianceHelper.areAllied(caster, target)
		);
	}

	private boolean intersectsCylinder(LivingEntity target, double radius) {
		AABB bounds = target.getBoundingBox();
		if (bounds.maxY < this.origin.y - BeatificPalliumData.EXPLOSION_VERTICAL_HALF_RANGE
				|| bounds.minY > this.origin.y + BeatificPalliumData.EXPLOSION_VERTICAL_HALF_RANGE) {
			return false;
		}

		double closestX = Mth.clamp(this.origin.x, bounds.minX, bounds.maxX);
		double closestZ = Mth.clamp(this.origin.z, bounds.minZ, bounds.maxZ);
		double dx = closestX - this.origin.x;
		double dz = closestZ - this.origin.z;
		return dx * dx + dz * dz <= radius * radius;
	}
}
