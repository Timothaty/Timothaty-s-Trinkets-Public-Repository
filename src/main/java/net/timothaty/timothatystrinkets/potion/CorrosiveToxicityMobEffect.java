package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.venom.VenomSphereData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class CorrosiveToxicityMobEffect extends MobEffect {
	private static final int PARTICLE_INTERVAL_TICKS = 6;
	private static final int PARTICLES_PER_STACK = 5;

	public CorrosiveToxicityMobEffect() {
		super(MobEffectCategory.HARMFUL, -16724941);
		this.addAttributeModifier(
				Attributes.ARMOR,
				ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "corrosive_toxicity_armor_reduction"),
				VenomSphereData.ARMOR_REDUCTION_PER_STACK,
				AttributeModifier.Operation.ADD_VALUE
		);
		this.addAttributeModifier(
				Attributes.MOVEMENT_SPEED,
				ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "corrosive_toxicity_speed_reduction"),
				VenomSphereData.SPEED_REDUCTION_PER_STACK,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel serverLevel)
				|| entity instanceof Player player && player.isSpectator()
				|| entity.tickCount % PARTICLE_INTERVAL_TICKS != 0) {
			return true;
		}

		int stacks = Mth.clamp(amplifier + 1, 1, VenomSphereData.MAX_STACKS);
		int count = stacks * PARTICLES_PER_STACK;
		double y = entity.getY() + entity.getBbHeight() * 0.78D;
		double horizontalSpread = Math.max(0.08D, entity.getBbWidth() * 0.36D);
		double verticalSpread = Math.max(0.06D, entity.getBbHeight() * 0.22D);
		serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.TOKSIK.get(),
				entity.getX(), y, entity.getZ(),
				count,
				horizontalSpread, verticalSpread, horizontalSpread,
				0.015D
		);
		return true;
	}
}
