package net.timothaty.timothatystrinkets.mechanics.champions_gauntlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyData;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.SoulEmpowerHelper;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingService;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingType;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.network.SoulRipTrailMessage;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsAttributeHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsSunPenaltyHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ChampionsGauntletEvents {
	private static final int SOUL_ABSORPTION_BLOOD_BITS = 22;
	private static final double SOUL_ABSORPTION_BLOOD_SPEED = 0.16D;
	private static final double SOUL_RIP_TRAIL_RENDER_DISTANCE = 64.0D;
	private static final Map<Player, Boolean> APPLIED_NIGHT_STATE = new WeakHashMap<>();

	private ChampionsGauntletEvents() {
	}

	public static void onCurioEquip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		ArmletGauntletSynergyState.invalidate(player);
		ArmletGauntletSynergyState.refreshFromCurios(player);
	}

	public static void onCurioUnequip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		ArmletGauntletSynergyState.invalidate(player);
	}

	public static void onMechanicalActiveStateChanged(Player player, boolean active) {
		if (player == null || player.level().isClientSide())
			return;
		applyPassiveModifiers(player, active);
		updateSoulAbsorptionModifiers(player, active && hasActiveSoulAbsorption(player));
		if (active)
			TimothatysTrinketsSunPenaltyHelper.applyNow(player, ChampionsGauntletData.SUN_PENALTY);
		else
			clearSoulAbsorptionIfActive(player);
	}

	public static boolean activateSoulAbsorption(Player player) {
		if (player == null || player.level().isClientSide())
			return false;
		if (player.isSpectator())
			return false;
		if (!TimothatysTrinketsEquipState.has(player, ChampionsGauntletData.EQUIP_STATE_KEY))
			return false;

		ItemStack gauntlet = ChampionsGauntletCurios.getGauntlet(player);
		if (gauntlet.isEmpty())
			return false;
		if (player.getCooldowns().isOnCooldown(gauntlet.getItem()))
			return false;

		spendFood(player, ChampionsGauntletData.SOUL_ABSORPTION_FOOD_COST);
		int soulEmpowerLevel = SoulEmpowerHelper.getLevel(player);
		float healthCost = ChampionsGauntletData.SOUL_ABSORPTION_HEALTH_COST
				+ (soulEmpowerLevel >= ArmletGauntletSynergyData.HIGH_COST_MIN_LEVEL ? ArmletGauntletSynergyData.HIGH_LEVEL_EXTRA_HEALTH_COST : 0.0F);
		spendHealth(player, healthCost);
		player.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.SOUL_ABSORPTION, ChampionsGauntletData.SOUL_ABSORPTION_DURATION_TICKS, 0, false, false, true));
		player.getPersistentData().putBoolean(ChampionsGauntletData.NBT_SOUL_ABSORPTION_ACTIVE, true);
		player.getPersistentData().putFloat(ChampionsGauntletData.NBT_PENDING_HEAL, 0.0F);
		player.getPersistentData().putInt(ChampionsGauntletData.NBT_KILL_COUNT, 0);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get(), ChampionsGauntletData.SOUL_ABSORPTION_COOLDOWN_TICKS);
		updateSoulAbsorptionModifiers(player, true);
		spawnSoulAbsorptionActivationBlood(player);
		playSoulAbsorptionSound(player);
		return true;
	}

	public static boolean hasActiveSoulAbsorption(Player player) {
		return player != null
				&& TimothatysTrinketsEquipState.has(player, ChampionsGauntletData.EQUIP_STATE_KEY)
				&& player.hasEffect(TimothatysTrinketsModMobEffects.SOUL_ABSORPTION)
				&& player.getPersistentData().getBoolean(ChampionsGauntletData.NBT_SOUL_ABSORPTION_ACTIVE);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		boolean hasGauntlet = TimothatysTrinketsEquipState.has(player, ChampionsGauntletData.EQUIP_STATE_KEY);
		if (hasGauntlet)
			updateNightHealthModifierIfNeeded(player);

		boolean soulAbsorptionActive = hasActiveSoulAbsorption(player);
		if (!hasGauntlet) {
			if (player.hasEffect(TimothatysTrinketsModMobEffects.SOUL_ABSORPTION)
					|| player.getPersistentData().getBoolean(ChampionsGauntletData.NBT_SOUL_ABSORPTION_ACTIVE))
				clearSoulAbsorptionIfActive(player);
			return;
		}

		TimothatysTrinketsSunPenaltyHelper.apply(player, ChampionsGauntletData.SUN_PENALTY);

		if (soulAbsorptionActive) {
			spawnSoulAbsorptionParticles(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive() || target instanceof ArmorStand)
			return;

		DamageSource source = event.getSource();
		if (!isDirectChargedPlayerAttack(source))
			return;

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof Player attacker))
			return;
		if (attacker.level().isClientSide() || target == attacker)
			return;
		if (!TimothatysTrinketsEquipState.has(attacker, ChampionsGauntletData.EQUIP_STATE_KEY))
			return;
		if (PactOfAllianceHelper.areAllied(attacker, target))
			return;

		boolean empowered = hasActiveSoulAbsorption(attacker);
		float procChance = ChampionsGauntletData.BASE_MAGIC_PROC_CHANCE;
		float magicMultiplier = ChampionsGauntletData.BASE_MAGIC_DAMAGE_MULTIPLIER;
		if (empowered) {
			procChance += ChampionsGauntletData.SOUL_ABSORPTION_MAGIC_PROC_CHANCE_BONUS;
			magicMultiplier += ChampionsGauntletData.SOUL_ABSORPTION_MAGIC_DAMAGE_BONUS;
		}

		if (attacker.getRandom().nextFloat() >= procChance)
			return;

		float magicDamage = event.getNewDamage() * magicMultiplier;
		if (empowered) {
			int soulEmpowerLevel = SoulEmpowerHelper.getLevel(attacker);
			magicDamage *= 1.0F + soulEmpowerLevel * ArmletGauntletSynergyData.MAGIC_DAMAGE_MULTIPLIER_PER_LEVEL;
		}
		if (magicDamage <= 0.0F)
			return;

		queueMagicDamage(attacker, target, magicDamage, empowered);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide() || victim instanceof ArmorStand)
			return;

		Entity attackerEntity = event.getSource().getEntity();
		if (!(attackerEntity instanceof Player killer))
			return;
		if (!hasActiveSoulAbsorption(killer))
			return;

		float pendingHeal = victim instanceof Player
				? ChampionsGauntletData.SOUL_ABSORPTION_PLAYER_KILL_PENDING_HEAL
				: ChampionsGauntletData.SOUL_ABSORPTION_MOB_KILL_PENDING_HEAL;
		addPendingHeal(killer, pendingHeal);
		incrementKillCount(killer);
		applyVoidMarkedDeathStun(killer, victim);
	}

	@SubscribeEvent
	public static void onSoulAbsorptionRemoved(MobEffectEvent.Remove event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (event.getEffect().value() != TimothatysTrinketsModMobEffects.SOUL_ABSORPTION.get())
			return;
		handleSoulAbsorptionEnd(player);
	}

	@SubscribeEvent
	public static void onSoulAbsorptionExpired(MobEffectEvent.Expired event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!isSoulAbsorption(event.getEffectInstance()))
			return;
		handleSoulAbsorptionEnd(player);
	}

	private static boolean isSoulAbsorption(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.SOUL_ABSORPTION.get();
	}

	private static void handleSoulAbsorptionEnd(Player player) {
		if (player == null || player.level().isClientSide())
			return;

		CompoundTag data = player.getPersistentData();
		boolean wasGauntletSoulAbsorption = data.getBoolean(ChampionsGauntletData.NBT_SOUL_ABSORPTION_ACTIVE);
		float pendingHeal = data.getFloat(ChampionsGauntletData.NBT_PENDING_HEAL);
		int killCount = data.getInt(ChampionsGauntletData.NBT_KILL_COUNT);
		data.remove(ChampionsGauntletData.NBT_SOUL_ABSORPTION_ACTIVE);
		data.remove(ChampionsGauntletData.NBT_PENDING_HEAL);
		data.remove(ChampionsGauntletData.NBT_KILL_COUNT);
		updateSoulAbsorptionModifiers(player, false);

		if (!wasGauntletSoulAbsorption)
			return;

		if (killCount <= 0 && player.isAlive()) {
			spendHealth(player, ChampionsGauntletData.SOUL_ABSORPTION_NO_KILL_HEALTH_COST);
		}
		if (pendingHeal > 0.0F && player.isAlive()) {
			RelicHealingService.heal(
					player,
					pendingHeal,
					RelicHealingType.SOUL
			);
		}
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ChampionsGauntletData.SOUL_ABSORPTION_WEAKNESS_TICKS, ChampionsGauntletData.SOUL_ABSORPTION_WEAKNESS_AMPLIFIER, false, true, true));
	}

	private static void applyPassiveModifiers(Player player, boolean shouldHaveModifiers) {
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.ARMOR,
				ChampionsGauntletData.ARMOR_MODIFIER_ID,
				ChampionsGauntletData.ARMOR_BONUS,
				AttributeModifier.Operation.ADD_VALUE,
				shouldHaveModifiers
		);
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.MAX_HEALTH,
				ChampionsGauntletData.MAX_HEALTH_MODIFIER_ID,
				getMaxHealthBonus(player),
				AttributeModifier.Operation.ADD_VALUE,
				shouldHaveModifiers
		);
		if (shouldHaveModifiers)
			APPLIED_NIGHT_STATE.put(player, player.level().isNight());
		else
			APPLIED_NIGHT_STATE.remove(player);
	}

	private static void updateNightHealthModifierIfNeeded(Player player) {
		boolean night = player.level().isNight();
		Boolean appliedNight = APPLIED_NIGHT_STATE.get(player);
		if (appliedNight != null && appliedNight == night)
			return;
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.MAX_HEALTH,
				ChampionsGauntletData.MAX_HEALTH_MODIFIER_ID,
				getMaxHealthBonus(player),
				AttributeModifier.Operation.ADD_VALUE,
				true
		);
		APPLIED_NIGHT_STATE.put(player, night);
	}

	private static double getMaxHealthBonus(Player player) {
		double bonus = ChampionsGauntletData.MAX_HEALTH_BONUS;
		if (player != null && player.level().isNight()) {
			bonus += ChampionsGauntletData.NIGHT_MAX_HEALTH_BONUS;
		}
		return bonus;
	}

	private static void updateSoulAbsorptionModifiers(Player player, boolean shouldHaveModifiers) {
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.MOVEMENT_SPEED,
				ChampionsGauntletData.SOUL_ABSORPTION_SPEED_MODIFIER_ID,
				ChampionsGauntletData.SOUL_ABSORPTION_SPEED_BONUS,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				shouldHaveModifiers
		);
	}

	private static boolean isDirectChargedPlayerAttack(DamageSource source) {
		if (source == null)
			return false;
		if (!source.is(DamageTypeTags.IS_PLAYER_ATTACK))
			return false;
		Entity attacker = source.getEntity();
		Entity direct = source.getDirectEntity();
		return attacker instanceof Player player && direct == attacker && player.getAttackStrengthScale(0.5F) >= ChampionsGauntletData.FULL_ATTACK_STRENGTH;
	}

	private static void clearSoulAbsorptionIfActive(Player player) {
		if (player == null)
			return;
		if (player.hasEffect(TimothatysTrinketsModMobEffects.SOUL_ABSORPTION)) {
			player.removeEffect(TimothatysTrinketsModMobEffects.SOUL_ABSORPTION);
		} else {
			player.getPersistentData().remove(ChampionsGauntletData.NBT_SOUL_ABSORPTION_ACTIVE);
			player.getPersistentData().remove(ChampionsGauntletData.NBT_PENDING_HEAL);
			player.getPersistentData().remove(ChampionsGauntletData.NBT_KILL_COUNT);
		}
	}

	private static void spendFood(Player player, float amount) {
		FoodData foodData = player.getFoodData();
		float remaining = Math.max(0.0F, amount);

		float saturation = foodData.getSaturationLevel();
		float saturationSpent = Math.min(saturation, remaining);
		if (saturationSpent > 0.0F) {
			foodData.setSaturation(Math.max(0.0F, saturation - saturationSpent));
			remaining -= saturationSpent;
		}

		if (remaining > 0.0F) {
			foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - (int) Math.ceil(remaining)));
		}
	}

	private static void spendHealth(Player player, float amount) {
		float newHealth = Math.max(ChampionsGauntletData.SOUL_ABSORPTION_MIN_HEALTH_AFTER_COST, player.getHealth() - Math.max(0.0F, amount));
		player.setHealth(newHealth);
	}

	private static void addPendingHeal(Player player, float amount) {
		CompoundTag data = player.getPersistentData();
		float current = data.getFloat(ChampionsGauntletData.NBT_PENDING_HEAL);
		data.putFloat(ChampionsGauntletData.NBT_PENDING_HEAL, current + Math.max(0.0F, amount));
	}

	private static void incrementKillCount(Player player) {
		CompoundTag data = player.getPersistentData();
		data.putInt(ChampionsGauntletData.NBT_KILL_COUNT, data.getInt(ChampionsGauntletData.NBT_KILL_COUNT) + 1);
	}

	private static void queueMagicDamage(Player attacker, LivingEntity target, float amount, boolean empowered) {
		if (!(attacker.level() instanceof ServerLevel server))
			return;

		server.getServer().execute(() -> {
			if (!attacker.isAlive() || !target.isAlive() || target.isRemoved())
				return;
			if (!TimothatysTrinketsEquipState.has(attacker, ChampionsGauntletData.EQUIP_STATE_KEY))
				return;
			if (PactOfAllianceHelper.areAllied(attacker, target))
				return;

			Vec3 movementBeforeDamage = target.getDeltaMovement();
			int invulnerableTimeBeforeDamage = target.invulnerableTime;
			target.invulnerableTime = 0;
			boolean damaged = target.hurt(target.damageSources().indirectMagic(attacker, attacker), amount);
			target.invulnerableTime = Math.max(target.invulnerableTime, invulnerableTimeBeforeDamage);

			if (!damaged)
				return;

			if (target.isAlive()) {
				target.setDeltaMovement(movementBeforeDamage);
				target.hurtMarked = false;
				target.hasImpulse = false;
			}
			spawnSoulRipTrailVfx(server, target, empowered);
			server.playSound(null, target.blockPosition(), TimothatysTrinketsModSounds.MAGICAL_HIT_PROC.get(), SoundSource.PLAYERS, 0.45F, 0.9F + attacker.getRandom().nextFloat() * 0.15F);
		});
	}

	private static void spawnSoulRipTrailVfx(ServerLevel serverLevel, LivingEntity target, boolean empowered) {
		SoulRipTrailMessage message = new SoulRipTrailMessage(
				target.getX(),
				target.getY(),
				target.getZ(),
				target.getBbWidth(),
				target.getBbHeight(),
				empowered
		);
		double maxDistanceSqr = SOUL_RIP_TRAIL_RENDER_DISTANCE * SOUL_RIP_TRAIL_RENDER_DISTANCE;
		for (ServerPlayer player : serverLevel.players()) {
			if (player.distanceToSqr(target) <= maxDistanceSqr) {
				PacketDistributor.sendToPlayer(player, message);
			}
		}
	}

	private static void applyVoidMarkedDeathStun(Player killer, LivingEntity victim) {
		if (!victim.hasEffect(TimothatysTrinketsModMobEffects.MARKED_BY_VOID))
			return;

		double radius = ChampionsGauntletData.VOID_MARKED_DEATH_STUN_RADIUS;
		AABB area = new AABB(
				victim.getX() - radius,
				victim.getY() - radius,
				victim.getZ() - radius,
				victim.getX() + radius,
				victim.getY() + radius,
				victim.getZ() + radius
		);

		List<LivingEntity> targets = victim.level().getEntitiesOfClass(LivingEntity.class, area, living -> {
			if (living == null || !living.isAlive())
				return false;
			if (living == victim || living == killer)
				return false;
			if (PactOfAllianceHelper.areAllied(killer, living))
				return false;
			return living.distanceToSqr(victim) <= radius * radius;
		});

		for (LivingEntity target : targets) {
			target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.STUNNED, ChampionsGauntletData.VOID_MARKED_DEATH_STUN_TICKS, 0, false, false, true), killer);
		}
	}

	private static void spawnSoulAbsorptionActivationBlood(Player player) {
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;

		double y = player.getY() + player.getBbHeight() * 0.55D;
		double horizontalSpread = Math.max(0.18D, player.getBbWidth() * 0.42D);
		double verticalSpread = Math.max(0.20D, player.getBbHeight() * 0.32D);
		serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
				player.getX(),
				y,
				player.getZ(),
				SOUL_ABSORPTION_BLOOD_BITS,
				horizontalSpread,
				verticalSpread,
				horizontalSpread,
				SOUL_ABSORPTION_BLOOD_SPEED
		);
	}

	private static void spawnSoulAbsorptionParticles(Player player) {
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;
		if (player.tickCount % 3 != 0)
			return;

		double y = player.getY() + player.getBbHeight() * (0.2D + player.getRandom().nextDouble() * 0.65D);
		double spread = Math.max(0.25D, player.getBbWidth() * 0.45D);
		serverLevel.sendParticles(
				ParticleTypes.SOUL_FIRE_FLAME,
				player.getX(),
				y,
				player.getZ(),
				2,
				spread,
				0.12D,
				spread,
				0.01D
		);
	}

	private static void playSoulAbsorptionSound(Player player) {
		player.level().playSound(null, player.blockPosition(), TimothatysTrinketsModSounds.DESOLATED.get(), SoundSource.PLAYERS, 0.6F, 1.15F);
	}
}
