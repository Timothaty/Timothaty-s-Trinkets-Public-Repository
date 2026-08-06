package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.api.damage.HolyDamageApi;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisStrikeResolver;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class BeatificPalliumDamageEvents {
	private static final float EPSILON = 0.0001F;

	private BeatificPalliumDamageEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity target = event.getEntity();
		if (!(target.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F)
			return;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD))
			return;

		DamageSource source = event.getSource();
		if (HolyDamageApi.isHoly(source) && source.getDirectEntity() instanceof BeatificPalliumEntity)
			return;

		BeatificPalliumState.Session session = BeatificPalliumState.activeSession(target);
		if (session == null)
			return;

		float incoming = event.getAmount();
		float absorbedNow = Math.min(incoming, session.remainingCapacity());
		if (absorbedNow <= 0.0F)
			return;

		float overflow = Math.max(0.0F, incoming - absorbedNow);
		session.addAbsorbed(absorbedNow);
		boolean meleeImpact = isMeleeImpact(source);
		BeatificPalliumImpactGeometry.Result impact = createImpactGeometry(target, source, meleeImpact);
		if (impact != null) {
			BeatificPalliumState.recordImpact(
					level,
					session,
					impact,
					absorbedNow,
					target.getRandom().nextInt()
			);
			playImpactSound(level, target, session);
		}

		if (overflow <= EPSILON) {
			consumeFullyAbsorbedArrow(source.getDirectEntity());
			event.setAmount(0.0F);
			if (impact != null && meleeImpact) {
				HubrisStrikeResolver.markDefended(target, source, HubrisStrikeResolver.DefenseKind.BEATIFIC_PALLIUM);
			}
			event.setCanceled(true);
		} else {
			event.setAmount(overflow);
		}

		if (session.accumulatedDamage >= BeatificPalliumData.CAPACITY - EPSILON)
			BeatificPalliumState.endFromCapacity(level, session, target);
	}

	private static void consumeFullyAbsorbedArrow(Entity directEntity) {
		if (!(directEntity instanceof AbstractArrow arrow) || directEntity instanceof ThrownTrident)
			return;

		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		arrow.setDeltaMovement(Vec3.ZERO);
		arrow.discard();
	}

	private static boolean isMeleeImpact(DamageSource source) {
		Entity direct = source.getDirectEntity();
		Entity causing = source.getEntity();
		return causing instanceof LivingEntity
				&& direct == causing
				&& !(direct instanceof Projectile)
				&& !source.is(DamageTypeTags.IS_PROJECTILE);
	}

	private static BeatificPalliumImpactGeometry.Result createImpactGeometry(
			LivingEntity target,
			DamageSource source,
			boolean melee) {
		Entity direct = source.getDirectEntity();
		if (!(direct instanceof AbstractArrow) && !melee)
			return null;

		return direct instanceof AbstractArrow arrow
				? BeatificPalliumImpactGeometry.forArrow(target, arrow)
				: BeatificPalliumImpactGeometry.forMelee(target, (LivingEntity) source.getEntity());
	}

	private static void playImpactSound(ServerLevel level, LivingEntity target, BeatificPalliumState.Session session) {
		long gameTime = level.getGameTime();
		if (session.lastImpactSoundGameTime != Long.MIN_VALUE
				&& gameTime - session.lastImpactSoundGameTime < BeatificPalliumData.IMPACT_SOUND_THROTTLE_TICKS)
			return;
		session.lastImpactSoundGameTime = gameTime;
		level.playSound(
				null,
				target.getX(),
				target.getY() + target.getBbHeight() * 0.5D,
				target.getZ(),
				TimothatysTrinketsModSounds.BEATIFIC_PALLIUM_IMPACT.get(),
				SoundSource.PLAYERS,
				0.80F,
				0.96F + target.getRandom().nextFloat() * 0.08F
		);
	}
}
