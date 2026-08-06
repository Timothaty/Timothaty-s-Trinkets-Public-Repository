package net.timothaty.timothatystrinkets.mechanics.flaming_ember;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@SuppressWarnings({"deprecation", "removal"})
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class FlamingEmberEvents {
	private FlamingEmberEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;
		if (level.getGameTime() % FlamingEmberData.PASSIVE_TICK_INTERVAL != 0L)
			return;

		ItemStack ember = getEquippedEmber(player);
		if (ember.isEmpty())
			return;

		double passiveHeat = FlamingEmberEnvironment.getPassiveHeatPerSecond(player);
		if (isOnCooldown(player, ember) && passiveHeat > 0.0D)
			passiveHeat = 0.0D;
		if (passiveHeat == 0.0D)
			return;

		FlamingEmberData.addHeat(ember, passiveHeat);
	}

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;

		LivingEntity target = event.getEntity();
		if (!(target instanceof Player player))
			return;
		if (player.level().isClientSide())
			return;

		ItemStack ember = getEquippedEmber(player);
		if (ember.isEmpty())
			return;

		FlamingEmberImpulse.recordDamage(player, ember, event.getNewDamage());

		if (player.hasEffect(MobEffects.FIRE_RESISTANCE) || isOnCooldown(player, ember))
			return;

		DamageSource source = event.getSource();
		if (source == null)
			return;

		double gainedHeat = getInstantHeatFromDamage(player, source);
		if (gainedHeat <= 0.0D)
			return;

		FlamingEmberData.addHeat(ember, gainedHeat);
		recordInstantHeatForOverheat(player, ember, gainedHeat);
	}

	private static ItemStack getEquippedEmber(Player player) {
		return TimothatysCuriosHelper.findCurio(player, TimothatysTrinketsModItems.FLAMING_EMBER.get());
	}

	private static boolean isOnCooldown(Player player, ItemStack ember) {
		return player.getCooldowns().isOnCooldown(ember.getItem());
	}

	private static double getInstantHeatFromDamage(Player player, DamageSource source) {
		if (isHeatMobDamage(source))
			return randomHeat(player, FlamingEmberData.HEAT_MOB_DAMAGE_MIN, FlamingEmberData.HEAT_MOB_DAMAGE_MAX);

		if (!isFireDamage(source))
			return 0.0D;

		if (isLavaDamage(player, source))
			return randomHeat(player, FlamingEmberData.LAVA_DAMAGE_MIN, FlamingEmberData.LAVA_DAMAGE_MAX);

		return randomHeat(player, FlamingEmberData.FIRE_DAMAGE_MIN, FlamingEmberData.FIRE_DAMAGE_MAX);
	}

	private static boolean isHeatMobDamage(DamageSource source) {
		if (source == null)
			return false;

		if (isHeatMob(source.getEntity()))
			return true;

		Entity directEntity = source.getDirectEntity();
		if (isHeatMob(directEntity))
			return true;

		if (directEntity instanceof Projectile projectile) {
			return isHeatMob(projectile.getOwner());
		}

		return false;
	}

	private static boolean isHeatMob(Entity entity) {
		return entity != null && entity.getType().is(FlamingEmberTags.HEAT_MOBS);
	}

	private static boolean isFireDamage(DamageSource source) {
		return source.is(DamageTypeTags.IS_FIRE)
				|| source.is(DamageTypes.IN_FIRE)
				|| source.is(DamageTypes.ON_FIRE)
				|| source.is(DamageTypes.LAVA)
				|| source.is(DamageTypes.HOT_FLOOR)
				|| source.is(DamageTypes.FIREBALL)
				|| source.is(DamageTypes.UNATTRIBUTED_FIREBALL);
	}

	private static boolean isLavaDamage(Player player, DamageSource source) {
		return source.is(DamageTypes.LAVA)
				|| player.isInLava()
				|| player.getFluidHeight(FluidTags.LAVA) > 0.0D;
	}

	private static double randomHeat(Player player, double min, double max) {
		if (max <= min)
			return min;
		return min + player.getRandom().nextDouble() * (max - min);
	}

	private static void recordInstantHeatForOverheat(Player player, ItemStack ember, double gainedHeat) {
		CompoundTag data = player.getPersistentData();
		long now = player.level().getGameTime();
		long windowStart = data.getLong(FlamingEmberData.NBT_DAMAGE_WINDOW_START_TICK);
		double windowHeat = data.getDouble(FlamingEmberData.NBT_DAMAGE_WINDOW_HEAT);

		if (windowStart <= 0L || now - windowStart > FlamingEmberData.OVERHEAT_WINDOW_TICKS) {
			windowStart = now;
			windowHeat = 0.0D;
		}

		windowHeat += gainedHeat;
		if (windowHeat > FlamingEmberData.OVERHEAT_HEAT_THRESHOLD) {
			player.getCooldowns().addCooldown(ember.getItem(), FlamingEmberData.OVERHEAT_COOLDOWN_TICKS);
			clearOverheatWindow(player);
			return;
		}

		data.putLong(FlamingEmberData.NBT_DAMAGE_WINDOW_START_TICK, windowStart);
		data.putDouble(FlamingEmberData.NBT_DAMAGE_WINDOW_HEAT, windowHeat);
	}

	private static void clearOverheatWindow(Player player) {
		CompoundTag data = player.getPersistentData();
		data.remove(FlamingEmberData.NBT_DAMAGE_WINDOW_START_TICK);
		data.remove(FlamingEmberData.NBT_DAMAGE_WINDOW_HEAT);
	}
}
