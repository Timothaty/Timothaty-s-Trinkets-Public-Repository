package net.timothaty.timothatystrinkets.mechanics.vampiric_fangs;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.champions_gauntlet.ChampionsGauntletData;
import net.timothaty.timothatystrinkets.mechanics.champions_gauntlet.ChampionsGauntletEvents;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingService;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingType;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDamageSources;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.VampiricFangsCurios;
import net.timothaty.timothatystrinkets.util.VampiricFangsData;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Optional;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class VampiricFangsEvents {
	private VampiricFangsEvents() {}

	public static final int INSATIABLE_COOLDOWN_TICKS = 20 * 60;
	public static final double INSATIABLE_RANGE = 3.0D;
	public static final String INSATIABLE_LAST_USED_KEY = "tt_insatiable_last_used";
	public static final String INSATIABLE_CLIENT_BURST_TICK_KEY = "tt_insatiable_client_burst_tick";

	private static final float BASE_LIFESTEAL = 0.12F;
	private static final float LOW_HEALTH_BONUS = 0.09F;
	private static final float LOW_HEALTH_THRESHOLD = 0.15F;
	private static final int HAEMORRHAGE_TICKS = 20 * 10;
	private static final float INSATIABLE_DAMAGE = 4.0F;
	private static final int BITE_DELAY_TICKS = 3;
	private static final double BITE_MAX_DISTANCE_SQR = 9.0D;
	private static final double KILL_FAMES_MULTIPLIER = 0.02D;
	private static final double HAEMORRHAGE_KILL_BONUS = 10.0D;
	private static final double INSATIABLE_BASE_COST = 30.0D;
	private static final double INSATIABLE_HEALTH_COST_MULTIPLIER = 0.03D;

	public static void onCurioEquip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		if (stack == null || stack.isEmpty())
			return;
		if (HolyRosariumHelper.isUnholyRelicSuppressed(player, stack)) {
			TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.VAMPIRIC_FANGS, false);
			return;
		}

		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.VAMPIRIC_FANGS, true);
	}

	public static void onCurioUnequip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.VAMPIRIC_FANGS, false);
	}

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide())
			return;
		if (victim instanceof ArmorStand)
			return;

		float dealt = event.getNewDamage();
		if (dealt <= 0.0F)
			return;

		if (event.getSource().is(DamageTypeTags.IS_FIRE))
			return;
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		if (event.getSource().is(TimothatysTrinketsDamageSources.SOUL_DAMAGE))
			return;

		Entity attackerEntity = event.getSource().getEntity();
		if (!(attackerEntity instanceof Player attacker))
			return;
		if (PactOfAllianceHelper.areAllied(attacker, victim))
			return;

		ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(attacker);
		if (equippedFangs.isEmpty())
			return;

		if (!isPhysicalWeaponDamage(attacker, event.getSource()))
			return;

		float lifesteal = BASE_LIFESTEAL;
		if (attacker.getHealth() / attacker.getMaxHealth() < LOW_HEALTH_THRESHOLD)
			lifesteal += LOW_HEALTH_BONUS;

		float heal = dealt * lifesteal;
		if (heal > 0.0F)
			RelicHealingService.heal(attacker, heal, RelicHealingType.VAMPIRISM);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide())
			return;
		Entity attackerEntity = event.getSource().getEntity();
		if (!(attackerEntity instanceof Player killer))
			return;
		if (PactOfAllianceHelper.areAllied(killer, victim))
			return;

		ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(killer);
		if (equippedFangs.isEmpty())
			return;

		double gained = Math.max(0.0D, victim.getMaxHealth() * KILL_FAMES_MULTIPLIER);
		if (victim.hasEffect(TimothatysTrinketsModMobEffects.HAEMORRHAGE))
			gained += HAEMORRHAGE_KILL_BONUS;
		if (ChampionsGauntletEvents.hasActiveSoulAbsorption(killer))
			gained *= ChampionsGauntletData.SOUL_ABSORPTION_FAMES_MULTIPLIER;
		VampiricFangsData.addFames(equippedFangs, gained);
	}

	public static boolean activateInsatiable(Player player) {
		if (player == null)
			return false;
		if (player.level().isClientSide())
			return false;
		debug(player, "START", ChatFormatting.GRAY);
		if (player.isSpectator()) {
			debug(player, "FAIL: SPECTATOR", ChatFormatting.RED);
			return false;
		}

		ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(player);
		if (equippedFangs.isEmpty()) {
			debug(player, "FAIL: NO_FANGS_IN_CURIOS", ChatFormatting.RED);
			return false;
		}

		if (player.getCooldowns().isOnCooldown(equippedFangs.getItem())) {
			debug(player, "FAIL: ITEM_COOLDOWN " + getInsatiableCooldownLeft(player) + " ticks", ChatFormatting.RED);
			return false;
		}

		LivingEntity target = findLookedAtTarget(player, INSATIABLE_RANGE);
		if (target == null) {
			debug(player, "FAIL: NO_TARGET", ChatFormatting.RED);
			return false;
		}
		if (PactOfAllianceHelper.areAllied(player, target)) {
			debug(player, "FAIL: PACT_ALLY", ChatFormatting.RED);
			return false;
		}

		double cost = getInsatiableCost(target);
		double fames = VampiricFangsData.getFames(equippedFangs);
		if (fames < cost) {
			debug(player, "FAIL: NOT_ENOUGH_FAMES " + VampiricFangsData.format(fames) + "/" + VampiricFangsData.format(cost), ChatFormatting.RED);
			return false;
		}

		long now = player.level().getGameTime();
		VampiricFangsData.setFames(equippedFangs, fames - cost);
		player.getPersistentData().putLong(INSATIABLE_LAST_USED_KEY, now);
		player.getPersistentData().putLong(INSATIABLE_CLIENT_BURST_TICK_KEY, now);
		player.getCooldowns().addCooldown(equippedFangs.getItem(), INSATIABLE_COOLDOWN_TICKS);
		dashToTarget(player, target);
		TimothatysTrinketsMod.queueServerWork(BITE_DELAY_TICKS, () -> performInsatiableBite(player, target));
		debug(player, "SUCCESS: target=" + target.getName().getString() + ", cost=" + VampiricFangsData.format(cost) + ", bite in " + BITE_DELAY_TICKS + " ticks", ChatFormatting.GREEN);
		return true;
	}

	public static double getInsatiableCost(LivingEntity target) {
		if (target == null)
			return INSATIABLE_BASE_COST;
		return INSATIABLE_BASE_COST + Math.max(0.0D, target.getMaxHealth() * INSATIABLE_HEALTH_COST_MULTIPLIER);
	}

	public static int getInsatiableCooldownLeft(Player player) {
		if (player == null)
			return 0;
		ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(player);
		if (equippedFangs.isEmpty())
			return 0;
		float cooldown = player.getCooldowns().getCooldownPercent(equippedFangs.getItem(), 0.0F);
		return Math.max(0, Math.round(cooldown * INSATIABLE_COOLDOWN_TICKS));
	}

	public static LivingEntity findLookedAtTarget(Player player, double range) {
		if (player == null)
			return null;

		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(range));
		Level level = player.level();

		double maxDistanceSqr = range * range;
		BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) {
			maxDistanceSqr = eye.distanceToSqr(blockHit.getLocation());
		}

		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.25D);
		LivingEntity bestTarget = null;
		double bestDistanceSqr = maxDistanceSqr;

		for (Entity entity : level.getEntities(player, searchBox, entity -> isValidInsatiableTarget(player, entity))) {
			AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.35D);
			Optional<Vec3> hit = hitBox.clip(eye, end);
			if (hitBox.contains(eye)) {
				if (0.0D < bestDistanceSqr) {
					bestTarget = (LivingEntity) entity;
					bestDistanceSqr = 0.0D;
				}
			} else if (hit.isPresent()) {
				double distanceSqr = eye.distanceToSqr(hit.get());
				if (distanceSqr < bestDistanceSqr) {
					bestTarget = (LivingEntity) entity;
					bestDistanceSqr = distanceSqr;
				}
			}
		}
		return bestTarget;
	}

	public static boolean isValidInsatiableTarget(Player player, Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return false;
		if (!living.isAlive() || living == player || living instanceof ArmorStand)
			return false;
		if (PactOfAllianceHelper.areAllied(player, living))
			return false;
		return !living.isSpectator();
	}

	private static boolean isPhysicalWeaponDamage(Player attacker, DamageSource source) {
		if (source == null)
			return false;
		if (isDirectMeleeDamage(attacker, source))
			return true;

		Entity directEntity = source.getDirectEntity();
		if (directEntity == attacker)
			return false;
		if (directEntity == null)
			return false;
		ItemStack main = attacker.getMainHandItem();
		ItemStack off = attacker.getOffhandItem();
		return isRangedWeapon(main) || isRangedWeapon(off);
	}

	private static boolean isDirectMeleeDamage(Player attacker, DamageSource source) {
		return source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() == attacker && source.getDirectEntity() == attacker;
	}

	private static boolean isRangedWeapon(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem || stack.getItem() instanceof ProjectileWeaponItem;
	}

	private static void dashToTarget(Player player, LivingEntity target) {
		Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.35D, 0.0D);
		Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.25D, 0.0D);
		Vec3 dir = targetCenter.subtract(playerCenter);
		if (dir.lengthSqr() <= 1.0E-6D)
			return;
		dir = dir.normalize();
		player.setDeltaMovement(dir.scale(1.45D).add(0.0D, 0.12D, 0.0D));
		player.hurtMarked = true;
	}

	private static void performInsatiableBite(Player player, LivingEntity target) {
		if (player == null || target == null)
			return;
		if (player.level().isClientSide())
			return;
		if (!player.isAlive() || !target.isAlive())
			return;
		if (player.distanceToSqr(target) > BITE_MAX_DISTANCE_SQR)
			return;
		if (PactOfAllianceHelper.areAllied(player, target))
			return;

		ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(player);
		if (equippedFangs.isEmpty())
			return;

		target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.HAEMORRHAGE, HAEMORRHAGE_TICKS, 0, false, true, true), player);
		target.hurt(player.damageSources().indirectMagic(player, player), INSATIABLE_DAMAGE);
		RelicHealingService.heal(
				player,
				INSATIABLE_DAMAGE * BASE_LIFESTEAL,
				RelicHealingType.VAMPIRISM
		);

		if (player.level() instanceof ServerLevel server) {
			server.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.75F, 0.8F + player.getRandom().nextFloat() * 0.2F);
		}
	}

	public static float getInsatiableCooldownProgress(Player player) {
		if (player == null)
			return 0.0F;
		ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(player);
		if (equippedFangs.isEmpty())
			return 0.0F;
		return Mth.clamp(player.getCooldowns().getCooldownPercent(equippedFangs.getItem(), 0.0F), 0.0F, 1.0F);
	}

	private static void debug(Player player, String text, ChatFormatting color) {
		TimothatysTrinketsDebug.send(player, "Fangs", text, color);
	}
}
