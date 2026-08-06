package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.api.damage.HolyDamageApi;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.particle.BabahParticleOptions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import org.joml.Vector3f;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class CherubimsWisdomDamageEvents {
	private CherubimsWisdomDamageEvents() {
	}

	// LOW composes with normal-priority systems such as Salt of the Earth using the final
	// ordinary incoming amount; each event still receives exactly one Wisdom multiplier.
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F)
			return;

		DamageSource source = event.getSource();
		if (HolyDamageApi.isHoly(source)) {
			if (source.getEntity() instanceof Player player && isWisdomActive(player))
				event.setAmount(event.getAmount() * CherubimsWisdomData.HOLY_MULTIPLIER);
			return;
		}

		Entity causingEntity = source.getEntity();
		Entity directEntity = source.getDirectEntity();
		if (source.is(DamageTypes.PLAYER_ATTACK)
				&& causingEntity instanceof Player player
				&& directEntity == player) {
			if (isWisdomActive(player))
				event.setAmount(event.getAmount() * CherubimsWisdomData.MELEE_MULTIPLIER);
			return;
		}

		if (!source.is(DamageTypeTags.IS_PROJECTILE)
				|| !(directEntity instanceof Projectile projectile)
				|| !(projectile.getOwner() instanceof Player owner)
				|| causingEntity != owner
				|| !projectile.getPersistentData().getBoolean(CherubimsWisdomData.EMPOWERED_PROJECTILE_TAG))
			return;

		event.setAmount(event.getAmount() * CherubimsWisdomData.RANGED_MULTIPLIER);
	}

	@SubscribeEvent
	public static void onLivingDamage(LivingDamageEvent.Post event) {
		LivingEntity target = event.getEntity();
		if (!(target.level() instanceof ServerLevel level)
				|| event.getNewDamage() <= 0.0F
				|| !isWisdomEmpoweredDamage(event.getSource())) {
			return;
		}

		BabahParticleOptions particle = new BabahParticleOptions(
				new Vector3f(
						(float) CherubimsWisdomData.HOLY_PARTICLE_R,
						(float) CherubimsWisdomData.HOLY_PARTICLE_G,
						(float) CherubimsWisdomData.HOLY_PARTICLE_B
				),
				0.27F
		);
		level.sendParticles(
				particle,
				target.getX(),
				target.getY() + target.getBbHeight() * 0.55D,
				target.getZ(),
				1,
				0.0D,
				0.0D,
				0.0D,
				0.0D
		);
		level.playSound(
				null,
				target.getX(),
				target.getY(),
				target.getZ(),
				TimothatysTrinketsModSounds.CHERUBIMS_WISDOM_HIT.get(),
				SoundSource.PLAYERS,
				1F,
				0.75F + target.getRandom().nextFloat() * 0.4F
		);
	}

	private static boolean isWisdomEmpoweredDamage(DamageSource source) {
		if (HolyDamageApi.isHoly(source))
			return source.getEntity() instanceof Player player && isWisdomActive(player);

		Entity causingEntity = source.getEntity();
		Entity directEntity = source.getDirectEntity();
		if (source.is(DamageTypes.PLAYER_ATTACK)
				&& causingEntity instanceof Player player
				&& directEntity == player) {
			return isWisdomActive(player);
		}

		return source.is(DamageTypeTags.IS_PROJECTILE)
				&& directEntity instanceof Projectile projectile
				&& projectile.getOwner() instanceof Player owner
				&& causingEntity == owner
				&& projectile.getPersistentData().getBoolean(CherubimsWisdomData.EMPOWERED_PROJECTILE_TAG);
	}

	private static boolean isWisdomActive(Player player) {
		return player != null
				&& player.isAlive()
				&& !player.isRemoved()
				&& player.hasEffect(TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM);
	}
}
