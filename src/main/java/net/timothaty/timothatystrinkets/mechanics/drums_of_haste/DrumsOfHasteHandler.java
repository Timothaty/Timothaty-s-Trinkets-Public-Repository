package net.timothaty.timothatystrinkets.mechanics.drums_of_haste;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteCurios;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteData;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteEffects;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteStacks;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteVfx;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class DrumsOfHasteHandler {
	private static final long SAFETY_SYNC_INTERVAL_TICKS = 40L;
	private static final long LOGIC_INTERVAL_TICKS = 5L;


	public static void onCurioEquip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;

		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.DRUMS_OF_HASTE, true);
		long now = player.level().getGameTime();
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_EQUIP_EXPIRE_TICK, now + DrumsOfHasteData.EQUIP_GRACE_TICKS);

		int stacks = DrumsOfHasteStacks.getStackFury(stack);
		DrumsOfHasteStacks.setStacks(player, stacks);
		DrumsOfHasteEffects.applyAttributeModifiers(player, stacks);
	}

	public static void onCurioUnequip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		DrumsOfHasteStacks.clearStackFury(stack);
		cleanup(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		Level level = player.level();
		if (level.isClientSide)
			return;

		long now = level.getGameTime();
		if ((now % SAFETY_SYNC_INTERVAL_TICKS) == 0L) {
			syncEquipState(player);
		}

		if (!TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.DRUMS_OF_HASTE))
			return;

		if ((now % LOGIC_INTERVAL_TICKS) != 0L)
			return;

		int oldStacks = DrumsOfHasteStacks.getStacks(player);
		int stacks = DrumsOfHasteStacks.decayIfNeeded(player, now);
		if (stacks != oldStacks) {
			DrumsOfHasteEffects.applyAttributeModifiers(player, stacks);
		}

		if (level instanceof ServerLevel sl) {
			DrumsOfHasteVfx.spawnByStacks(sl, player, stacks, now);
		}
	}

	private static void syncEquipState(Player player) {
		boolean actual = DrumsOfHasteCurios.isDrumsEquipped(player);
		boolean cached = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.DRUMS_OF_HASTE);
		if (actual == cached)
			return;

		if (actual) {
			onCurioEquip(player, DrumsOfHasteCurios.getEquippedDrumsStack(player));
		} else {
			cleanup(player);
		}
	}

	private static void cleanup(Player player) {
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.DRUMS_OF_HASTE, false);
		player.getPersistentData().remove(DrumsOfHasteData.NBT_EQUIP_EXPIRE_TICK);
		DrumsOfHasteStacks.setStacks(player, 0);
		DrumsOfHasteStacks.resetTimers(player);
		DrumsOfHasteEffects.applyAttributeModifiers(player, 0);
	}

	@SubscribeEvent
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null)
			return;

		DamageSource source = event.getSource();
		if (source == null)
			return;

		if (!(event.getEntity() instanceof Player victim))
			return;

		Level level = victim.level();
		if (level.isClientSide || event.getNewDamage() <= 0.0F)
			return;

		if (!TimothatysTrinketsEquipState.has(victim, TimothatysTrinketsEquipState.DRUMS_OF_HASTE))
			return;

		long now = level.getGameTime();
		victim.getPersistentData().putLong(DrumsOfHasteData.NBT_LAST_DAMAGE_TICK, now);
		victim.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK, 0L);

		if (!isValidMeleeStackSource(source, victim))
			return;

		int oldStacks = DrumsOfHasteStacks.getStacks(victim);
		int newStacks = DrumsOfHasteData.clampStacks(oldStacks + 1);
		if (newStacks <= oldStacks) {
			DrumsOfHasteEffects.applyAttributeModifiers(victim, oldStacks);
			return;
		}

		DrumsOfHasteStacks.setStacks(victim, newStacks);
		DrumsOfHasteEffects.applyAttributeModifiers(victim, newStacks);
		playStackSoundServer(victim, newStacks, now);

		if (level instanceof ServerLevel sl) {
			DrumsOfHasteVfx.spawnDrumBeatOnce(sl, victim, newStacks);
		}
	}

	@SubscribeEvent
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide)
			return;

		if (!TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.DRUMS_OF_HASTE))
			return;

		int stacks = DrumsOfHasteStacks.getStacks(player);
		if (stacks <= 0)
			return;

		event.setNewSpeed(event.getNewSpeed() * DrumsOfHasteEffects.getBreakSpeedMultiplier(stacks));
	}

	private static boolean isValidMeleeStackSource(DamageSource source, Player victim) {
		boolean melee = source.is(DamageTypes.PLAYER_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
		if (!melee)
			return false;

		Entity attackerEntity = source.getEntity();
		Entity direct = source.getDirectEntity();
		if (attackerEntity == null || direct == null || direct != attackerEntity)
			return false;
		if (!(attackerEntity instanceof LivingEntity attacker))
			return false;
		if (!(attacker instanceof Player) && !(attacker instanceof Enemy))
			return false;
		return attacker != victim;
	}

	private static void playStackSoundServer(Player player, int stacks, long nowTick) {
		long last = player.getPersistentData().getLong(DrumsOfHasteData.NBT_LAST_STACK_SOUND_TICK);
		if (last == nowTick)
			return;
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_LAST_STACK_SOUND_TICK, nowTick);

		float volume = 0.8F;
		float pitch = 0.95F + (stacks * 0.01F);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TimothatysTrinketsModSounds.DRUM_STACKING.get(), SoundSource.PLAYERS, volume, pitch);
	}
}
