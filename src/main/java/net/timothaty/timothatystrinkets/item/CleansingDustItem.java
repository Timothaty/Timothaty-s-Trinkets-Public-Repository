package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.advancement.TimothatysTrinketsCriteriaTriggers;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.cleansing.CleansingZoneManager;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityDeedType;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityQuestService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class CleansingDustItem extends Item {

	private static final int UNDEAD_DAMAGE_COOLDOWN_TICKS = 20 * 25;
	private static final int CLEANSE_COOLDOWN_TICKS = 20 * 15;
	private static final int ZONE_COOLDOWN_TICKS = 20 * 60;
	private static final int WEAKNESS_TICKS = 20 * 15;
	private static final float UNDEAD_MAGIC_DAMAGE = 10.0F;
	private static final TagKey<MobEffect> CLEANSING_DUST_BLACKLIST = TagKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "cleansing_dust_blacklist"));

	public CleansingDustItem() {
		super(new Item.Properties().durability(6));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();
		BlockPos clickedPos = context.getClickedPos();

		if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
			if (isOnCooldown(player)) {
				return InteractionResult.FAIL;
			}
			CleansingZoneManager.createZone(serverLevel, player, clickedPos);
			playUseFx(level, player);
			damageNoBreakSound(player, context.getItemInHand(), 2);
			startCooldown(player, ZONE_COOLDOWN_TICKS);
			triggerAdvancement(player);
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!level.isClientSide) {
			if (isOnCooldown(player)) {
				return InteractionResultHolder.fail(stack);
			}
			removeAllHarmfulEffects(player);
			playUseFx(level, player);

			damageNoBreakSound(player, stack);
			startCooldown(player, CLEANSE_COOLDOWN_TICKS);
			triggerAdvancement(player);
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		Level level = player.level();
		if (!level.isClientSide) {
			if (isOnCooldown(player)) {
				return InteractionResult.FAIL;
			}

			boolean undead = target.isInvertedHealAndHarm();
			List<Holder<MobEffect>> removedEffects = removeAllHarmfulEffects(target);
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0, false, true, true));
			if (target instanceof Villager
					&& player instanceof ServerPlayer serverPlayer
					&& removedEffects.stream().anyMatch(effect -> effect.is(TimothatysTrinketsModMobEffects.UNDEADIFICATION) || effect.is(TimothatysTrinketsModMobEffects.PUTREFACTION)))
				HumilityQuestService.recordDeed(serverPlayer.getServer(), serverPlayer.getUUID(), HumilityDeedType.CURE_VILLAGER);

			if (undead) {
				target.hurt(player.damageSources().magic(), UNDEAD_MAGIC_DAMAGE);
			}

			playUseFx(level, target);

			damageNoBreakSound(player, stack);
			startCooldown(player, undead ? UNDEAD_DAMAGE_COOLDOWN_TICKS : CLEANSE_COOLDOWN_TICKS);
			triggerAdvancement(player);
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xFF46ED;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int max = this.getMaxDamage(stack);
		int damage = stack.getDamageValue();

		if (max <= 0) return 0;

		return Math.round(13.0F * (1.0F - (float) damage / (float) max));
	}

	private static List<Holder<MobEffect>> removeAllHarmfulEffects(LivingEntity entity) {
		List<Holder<MobEffect>> removedEffects = new ArrayList<>();
		var effectsCopy = new ArrayList<>(entity.getActiveEffects());
		for (MobEffectInstance inst : effectsCopy) {
			if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && !inst.getEffect().is(CLEANSING_DUST_BLACKLIST)) {
				if (entity.removeEffect(inst.getEffect()))
					removedEffects.add(inst.getEffect());
			}
		}
		return removedEffects;
	}

	private static void playUseFx(Level level, LivingEntity entity) {
		if (!(level instanceof ServerLevel server)) return;

		double x = entity.getX();
		double y = entity.getY() + entity.getBbHeight() * 0.5D;
		double z = entity.getZ();

		server.sendParticles(
				TimothatysTrinketsModParticleTypes.CLEANSING_DUST_PARTICLE.get(),
				x, y, z,
				40,
				0.35D, 0.45D, 0.35D,
				0.02D
		);

		level.playSound(
				null,
				entity.blockPosition(),
				TimothatysTrinketsModSounds.CLEANSING_DUST_USE.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);
	}

	private static void damageNoBreakSound(Player player, ItemStack stack) {
		damageNoBreakSound(player, stack, 1);
	}

	private static void damageNoBreakSound(Player player, ItemStack stack, int amount) {
		if (stack.isEmpty()) return;
		if (player.getAbilities().instabuild) return;
		if (amount <= 0) return;

		int newDamage = stack.getDamageValue() + amount;
		if (newDamage >= stack.getMaxDamage()) {
			stack.shrink(1);
		} else {
			stack.setDamageValue(newDamage);
		}
	}

	private static void triggerAdvancement(Player player) {
		if (player instanceof ServerPlayer serverPlayer)
			TimothatysTrinketsCriteriaTriggers.triggerUseCleansingDust(serverPlayer);
	}

	private boolean isOnCooldown(Player player) {
		return player.getCooldowns().isOnCooldown(this);
	}

	private void startCooldown(Player player, int durationTicks) {
		player.getCooldowns().addCooldown(this, durationTicks);
	}
}
