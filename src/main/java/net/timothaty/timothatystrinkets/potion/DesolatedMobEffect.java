package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.effects.DesolatedEffectEvents;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDamageSources;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public class DesolatedMobEffect extends MobEffect {
	public static final int EFFECT_COLOR = 0x010202;

	private static final long DAMAGE_INTERVAL_TICKS = 60L;
	private static final float SOUL_DAMAGE_AMOUNT = 2.0F;
	private static final String NBT_NEXT_SOUL_DAMAGE_TICK = "ttr_desolated_next_soul_damage_tick";

	private static final ResourceLocation DESOLATED_SOUND_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "desolated");
	private static final float DESOLATED_SOUND_VOLUME = 1.0F;
	private static final float DESOLATED_SOUND_PITCH = 1.0F;

	public DesolatedMobEffect() {
		super(MobEffectCategory.HARMFUL, EFFECT_COLOR);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		super.onEffectStarted(entity, amplifier);

		Level level = entity.level();
		if (!level.isClientSide()) {
			entity.getPersistentData().putLong(NBT_NEXT_SOUL_DAMAGE_TICK, level.getGameTime() + DAMAGE_INTERVAL_TICKS);
			playDesolatedSound(entity, level);
		}
	}

	private static void playDesolatedSound(LivingEntity entity, Level level) {
		SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(DESOLATED_SOUND_ID);
		if (soundEvent == null)
			return;

		level.playSound(
				null,
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				soundEvent,
				SoundSource.HOSTILE,
				DESOLATED_SOUND_VOLUME,
				DESOLATED_SOUND_PITCH
		);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		Level level = entity.level();
		if (level.isClientSide())
			return true;

		DesolatedEffectEvents.tickEffect(entity);

		if (!entity.isAlive() || isCreativeLike(entity))
			return true;

		long now = level.getGameTime();
		long nextDamageTick = entity.getPersistentData().getLong(NBT_NEXT_SOUL_DAMAGE_TICK);

		if (nextDamageTick <= 0L) {
			entity.getPersistentData().putLong(NBT_NEXT_SOUL_DAMAGE_TICK, now + DAMAGE_INTERVAL_TICKS);
			return true;
		}

		if (now >= nextDamageTick) {
			hurtWithSoulDamageWithoutKnockback(entity, level);
			entity.getPersistentData().putLong(NBT_NEXT_SOUL_DAMAGE_TICK, now + DAMAGE_INTERVAL_TICKS);
		}

		return true;
	}

	private static void hurtWithSoulDamageWithoutKnockback(LivingEntity entity, Level level) {
		Vec3 movementBeforeDamage = entity.getDeltaMovement();
		boolean wasHurt = entity.hurt(TimothatysTrinketsDamageSources.soulDamage(level), SOUL_DAMAGE_AMOUNT);

		if (wasHurt && entity.isAlive()) {
			entity.setDeltaMovement(movementBeforeDamage);
			entity.hurtMarked = false;
			entity.hasImpulse = false;
		}
	}

	private static boolean isCreativeLike(LivingEntity entity) {
		return entity instanceof Player player && (player.isCreative() || player.isSpectator());
	}

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
	public static final class ClientExtensions {
		@SubscribeEvent
		public static void registerMobEffectExtensions(RegisterClientExtensionsEvent event) {
			event.registerMobEffect(new IClientMobEffectExtensions() {
				@Override
				public boolean isVisibleInGui(MobEffectInstance effect) {
					return true;
				}
			}, TimothatysTrinketsModMobEffects.DESOLATED.get());
		}
	}
}
