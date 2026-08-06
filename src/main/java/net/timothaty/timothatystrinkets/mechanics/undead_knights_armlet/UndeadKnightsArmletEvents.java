package net.timothaty.timothatystrinkets.mechanics.undead_knights_armlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;
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
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class UndeadKnightsArmletEvents {
	private static final int SOUL_HUNGER_BLOOD_BITS = 22;
	private static final double SOUL_HUNGER_BLOOD_SPEED = 0.16D;
	private static final double SOUL_RIP_TRAIL_RENDER_DISTANCE = 64.0D;

	private UndeadKnightsArmletEvents() {
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

	public static void onMechanicalActiveStateChanged(Player player, boolean soloActive) {
		if (player == null || player.level().isClientSide())
			return;
		applyPassiveModifiers(player, soloActive);
		updateSoulHungerModifiers(player, soloActive && hasActiveSoulHunger(player));
		if (soloActive)
			TimothatysTrinketsSunPenaltyHelper.applyNow(player, UndeadKnightsArmletData.SUN_PENALTY);
		else
			clearSoulHungerIfActive(player);
	}

	public static boolean activateSoulHunger(Player player) {
		if (player == null || player.level().isClientSide())
			return false;
		if (player.isSpectator())
			return false;
		if (!isArmletSoloActive(player))
			return false;

		ItemStack armlet = UndeadKnightsArmletCurios.getArmlet(player);
		if (armlet.isEmpty())
			return false;
		if (player.getCooldowns().isOnCooldown(armlet.getItem()))
			return false;

		spendFood(player, UndeadKnightsArmletData.SOUL_HUNGER_FOOD_COST);
		spendHealth(player, UndeadKnightsArmletData.SOUL_HUNGER_HEALTH_COST);
		player.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.SOUL_HUNGER, UndeadKnightsArmletData.SOUL_HUNGER_DURATION_TICKS, 0, false, false, true));
		player.getPersistentData().putBoolean(UndeadKnightsArmletData.NBT_SOUL_HUNGER_ACTIVE, true);
		player.getPersistentData().putFloat(UndeadKnightsArmletData.NBT_PENDING_HEAL, 0.0F);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get(), UndeadKnightsArmletData.SOUL_HUNGER_COOLDOWN_TICKS);
		updateSoulHungerModifiers(player, true);
		spawnSoulHungerActivationBlood(player);
		playSoulHungerSound(player);
		return true;
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		boolean hasArmlet = isArmletSoloActive(player);

		boolean soulHungerActive = hasActiveSoulHunger(player);
		if (!hasArmlet) {
			if (player.hasEffect(TimothatysTrinketsModMobEffects.SOUL_HUNGER)
					|| player.getPersistentData().getBoolean(UndeadKnightsArmletData.NBT_SOUL_HUNGER_ACTIVE))
				clearSoulHungerIfActive(player);
			return;
		}

		TimothatysTrinketsSunPenaltyHelper.apply(player, UndeadKnightsArmletData.SUN_PENALTY);

		if (soulHungerActive) {
			spawnSoulHungerParticles(player);
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
		if (!isArmletSoloActive(attacker))
			return;
		if (PactOfAllianceHelper.areAllied(attacker, target))
			return;

		boolean empowered = hasActiveSoulHunger(attacker);
		float procChance = UndeadKnightsArmletData.BASE_MAGIC_PROC_CHANCE;
		float magicMultiplier = UndeadKnightsArmletData.BASE_MAGIC_DAMAGE_MULTIPLIER;
		if (empowered) {
			procChance += UndeadKnightsArmletData.SOUL_HUNGER_MAGIC_PROC_CHANCE_BONUS;
			magicMultiplier += UndeadKnightsArmletData.SOUL_HUNGER_MAGIC_DAMAGE_BONUS;
		}

		if (attacker.getRandom().nextFloat() >= procChance)
			return;

		float magicDamage = event.getNewDamage() * magicMultiplier;
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
		if (!isArmletSoloActive(killer) || !hasActiveSoulHunger(killer))
			return;

		float pendingHeal = victim instanceof Player
				? UndeadKnightsArmletData.SOUL_HUNGER_PLAYER_KILL_PENDING_HEAL_MIN + killer.getRandom().nextFloat() * (UndeadKnightsArmletData.SOUL_HUNGER_PLAYER_KILL_PENDING_HEAL_MAX - UndeadKnightsArmletData.SOUL_HUNGER_PLAYER_KILL_PENDING_HEAL_MIN)
				: UndeadKnightsArmletData.SOUL_HUNGER_MOB_KILL_PENDING_HEAL;
		addPendingHeal(killer, pendingHeal);
	}

	@SubscribeEvent
	public static void onSoulHungerRemoved(MobEffectEvent.Remove event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (event.getEffect().value() != TimothatysTrinketsModMobEffects.SOUL_HUNGER.get())
			return;
		handleSoulHungerEnd(player);
	}

	@SubscribeEvent
	public static void onSoulHungerExpired(MobEffectEvent.Expired event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!isSoulHunger(event.getEffectInstance()))
			return;
		handleSoulHungerEnd(player);
	}

	private static boolean isSoulHunger(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.SOUL_HUNGER.get();
	}

	private static void handleSoulHungerEnd(Player player) {
		if (player == null || player.level().isClientSide())
			return;

		boolean wasArmletSoulHunger = player.getPersistentData().getBoolean(UndeadKnightsArmletData.NBT_SOUL_HUNGER_ACTIVE);
		float pendingHeal = player.getPersistentData().getFloat(UndeadKnightsArmletData.NBT_PENDING_HEAL);
		player.getPersistentData().remove(UndeadKnightsArmletData.NBT_SOUL_HUNGER_ACTIVE);
		player.getPersistentData().remove(UndeadKnightsArmletData.NBT_PENDING_HEAL);
		updateSoulHungerModifiers(player, false);

		if (!wasArmletSoulHunger)
			return;

		if (pendingHeal > 0.0F && player.isAlive()) {
			RelicHealingService.heal(
					player,
					pendingHeal,
					RelicHealingType.SOUL
			);
		}
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, UndeadKnightsArmletData.SOUL_HUNGER_WEAKNESS_TICKS, UndeadKnightsArmletData.SOUL_HUNGER_WEAKNESS_AMPLIFIER, false, true, true));
	}

	private static void applyPassiveModifiers(Player player, boolean shouldHaveModifier) {
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.ARMOR,
				UndeadKnightsArmletData.ARMOR_MODIFIER_ID,
				UndeadKnightsArmletData.ARMOR_BONUS,
				AttributeModifier.Operation.ADD_VALUE,
				shouldHaveModifier
		);
	}

	private static void updateSoulHungerModifiers(Player player, boolean shouldHaveModifiers) {
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.MOVEMENT_SPEED,
				UndeadKnightsArmletData.SOUL_HUNGER_SPEED_MODIFIER_ID,
				UndeadKnightsArmletData.SOUL_HUNGER_SPEED_BONUS,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				shouldHaveModifiers
		);
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.ATTACK_SPEED,
				UndeadKnightsArmletData.SOUL_HUNGER_ATTACK_SPEED_MODIFIER_ID,
				UndeadKnightsArmletData.SOUL_HUNGER_ATTACK_SPEED_PENALTY,
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
		return attacker instanceof Player player && direct == attacker && player.getAttackStrengthScale(0.5F) >= UndeadKnightsArmletData.FULL_ATTACK_STRENGTH;
	}

	private static boolean hasActiveSoulHunger(Player player) {
		return isArmletSoloActive(player)
				&& player.hasEffect(TimothatysTrinketsModMobEffects.SOUL_HUNGER)
				&& player.getPersistentData().getBoolean(UndeadKnightsArmletData.NBT_SOUL_HUNGER_ACTIVE);
	}

	private static boolean isArmletActive(Player player) {
		return TimothatysTrinketsEquipState.has(player, UndeadKnightsArmletData.EQUIP_STATE_KEY);
	}

	private static boolean isArmletSoloActive(Player player) {
		return isArmletActive(player)
				&& !TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET);
	}

	private static void clearSoulHungerIfActive(Player player) {
		if (player == null)
			return;
		if (player.hasEffect(TimothatysTrinketsModMobEffects.SOUL_HUNGER)) {
			player.removeEffect(TimothatysTrinketsModMobEffects.SOUL_HUNGER);
		} else {
			player.getPersistentData().remove(UndeadKnightsArmletData.NBT_SOUL_HUNGER_ACTIVE);
			player.getPersistentData().remove(UndeadKnightsArmletData.NBT_PENDING_HEAL);
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
		float newHealth = Math.max(UndeadKnightsArmletData.SOUL_HUNGER_MIN_HEALTH_AFTER_COST, player.getHealth() - Math.max(0.0F, amount));
		player.setHealth(newHealth);
	}

	private static void addPendingHeal(Player player, float amount) {
		CompoundTag data = player.getPersistentData();
		float current = data.getFloat(UndeadKnightsArmletData.NBT_PENDING_HEAL);
		data.putFloat(UndeadKnightsArmletData.NBT_PENDING_HEAL, current + Math.max(0.0F, amount));
	}

	private static void queueMagicDamage(Player attacker, LivingEntity target, float amount, boolean empowered) {
		if (!(attacker.level() instanceof ServerLevel server))
			return;

		server.getServer().execute(() -> {
			if (!attacker.isAlive() || !target.isAlive() || target.isRemoved())
				return;
			if (!isArmletSoloActive(attacker))
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

	private static void spawnSoulHungerActivationBlood(Player player) {
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
				SOUL_HUNGER_BLOOD_BITS,
				horizontalSpread,
				verticalSpread,
				horizontalSpread,
				SOUL_HUNGER_BLOOD_SPEED
		);
	}

	private static void spawnSoulHungerParticles(Player player) {
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

	private static void playSoulHungerSound(Player player) {
		player.level().playSound(null, player.blockPosition(), TimothatysTrinketsModSounds.DESOLATED.get(), SoundSource.PLAYERS, 0.6F, 1.15F);
	}
}
