package net.timothaty.timothatystrinkets.mechanics.damnation_altar;


import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.advancement.TimothatysTrinketsCriteriaTriggers;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightSpreadHelper;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordSummonManager;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordProgressionHandler;
import net.timothaty.timothatystrinkets.util.DamnationAltarOfferDisplayHandler;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class DamnationAltarSacrificeHandler {

	private static final float OFFER_CHANCE = 0.3f;
	private static final float BASE_CORRECT_DAMAGE_CURSE_CHANCE = 0.17f;
	private static final float BASE_CORRECT_BLIGHT_CURSE_CHANCE = 0.17f;

	private static final int ALTARS_CURSE_DURATION_TICKS = 60 * 60;
	private static final int ALTARS_CURSE_MAX_LEVEL = 5;
	private static final int CORRECT_BLIGHT_RADIUS = 12;
	private static final int CORRECT_BLIGHT_VERTICAL_RADIUS = 6;
	private static final int CORRECT_BLIGHT_BLOCKS_MIN = 2;
	private static final int CORRECT_BLIGHT_BLOCKS_MAX = 3;
	private static final int PLAYER_SACRIFICE_BLOOD_DURATION_TICKS = 20 * 4;

	private static final TagKey<Item> RARE_ITEM_TAG = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "rare_items")
	);

	private static final SoundEvent SOUND_COMMON_ITEM = TimothatysTrinketsModSounds.COMMON_ITEM_SACRIFICE.get();
	private static final SoundEvent SOUND_RARE_ITEM = TimothatysTrinketsModSounds.RARE_ITEM_SACRIFICE.get();
	private static final SoundEvent SOUND_BLOOD_BOIL = TimothatysTrinketsModSounds.BLOOD_BOIL.get();

	public static boolean handle(ServerLevel level, ServerPlayer player, LivingEntity victim, BlockPos altarPos) {
		ItemStack offhand = player.getOffhandItem();
		boolean hasDagger = isRitualDagger(offhand);

		if (level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar && altar.isBusyForExternalRitual())
			return false;
		if (DebtlordSummonManager.isAltarActive(level, altarPos))
			return false;
		if (!hasDagger)
			return false;

		if (victim instanceof Villager) {
			if (DebtlordProgressionHandler.hasDefeatedDebtlord(player)) {
				player.displayClientMessage(Component.translatable("altar.timohatys_trinkets.debtlord_already_defeated"), true);
				return false;
			}
			if (!DamnationAltarSacrificeRouter.tryConsumeSacrifice(level, altarPos)) return false;
			handleBloodBoilingSacrifice(level, player, victim, altarPos);
			return DebtlordSummonManager.handleVillagerSacrifice(level, player, victim, altarPos, offhand);
		}

		if (!DamnationAltarSacrificeRouter.tryConsumeSacrifice(level, altarPos))
			return false;

		if (victim instanceof Player) {
			damageRitualDagger(player, offhand);
			handlePlayerSacrifice(level, player, victim, altarPos);
			return true;
		}

		if (victim instanceof Sheep sheep) {
			ItemStack sacrificeTool = offhand.copy();
			damageRitualDagger(player, offhand);
			SheepOfferingProfile profile = SheepOfferingProfile.fromSheep(sheep);
			if (profile != null) {
				handleCorrectSheepSacrifice(level, player, victim, altarPos, profile, sacrificeTool);
			} else {
				handleWrongSacrifice(level, player, altarPos);
			}
			TimothatysTrinketsCriteriaTriggers.triggerSacrificeSheep(player);
			return true;
		}

		damageRitualDagger(player, offhand);
		handleWrongSacrifice(level, player, altarPos);
		return true;
	}

	private static void handlePlayerSacrifice(ServerLevel level, ServerPlayer player, LivingEntity victim, BlockPos altarPos) {
		handleBloodBoilingSacrifice(level, player, victim, altarPos);

		int delta = 15 + level.getRandom().nextInt(4);
		int rel = setRelationClamped(player, getOrInitRelation(player) + delta);
		debug(player, "Player sacrifice. Relation=" + rel + " (+" + delta + ")");
	}

	private static void handleBloodBoilingSacrifice(ServerLevel level, ServerPlayer player, LivingEntity victim, BlockPos altarPos) {
		spawnBloodParticles(level, victim);
		DamnationAltarSacrificeRouter.startBloodRitual(level, altarPos, PLAYER_SACRIFICE_BLOOD_DURATION_TICKS);
		float pitch = 0.9F + level.getRandom().nextFloat() * 0.4F;
		playAtAltar(level, altarPos, SOUND_BLOOD_BOIL, SoundSource.BLOCKS, 1.0F, pitch);
		TimothatysTrinketsCriteriaTriggers.triggerBloodBoilingSacrifice(player);
	}

	private static void handleCorrectSheepSacrifice(ServerLevel level, ServerPlayer player, LivingEntity victim, BlockPos altarPos,
			SheepOfferingProfile profile, ItemStack sacrificeTool) {
		spawnBloodParticles(level, victim);
		spawnSacrificeSuccessVfx(level, altarPos);

		RandomSource random = level.getRandom();
		boolean damageCurse = rollChance(random, BASE_CORRECT_DAMAGE_CURSE_CHANCE + profile.damageCurseBonus());
		boolean blightCurse = rollChance(random, BASE_CORRECT_BLIGHT_CURSE_CHANCE + profile.blightCurseBonus());

		if (damageCurse) {
			int strikes = random.nextBoolean() ? 1 : 4;
			DamnationAltarPunishmentService.scheduleSacrificePunishment(level, player, altarPos, victim, strikes);
			debug(player, "Sheep curse: punishment strikes=" + strikes);
		}

		if (blightCurse) {
			int infected = tryInfectCorrectBlightNearAltar(level, altarPos);
			debug(player, "Sheep curse: blight blocks=" + infected);
		}

		if (profile.strengthChance() > 0.0F && rollChance(random, profile.strengthChance())) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60 * 20, 0, false, true, true));
			debug(player, "Red sheep blessing: strength");
		}

		if (profile.spawnNecroSummons()) {
			int scheduled = DamnationAltarSummonTelegraphManager.schedule(level, altarPos, 1 + random.nextInt(2));
			debug(player, "Gray sheep pending summons=" + scheduled);
		}

		if (profile.forceOffer() || random.nextFloat() < OFFER_CHANCE) {
			ItemStack offerStack = DamnationAltarLootService.rollOffer(level, player, altarPos, sacrificeTool);
			if (DamnationAltarOfferDisplayHandler.tryCreateOfferFromStack(level, altarPos, offerStack)) {
				playAtAltar(level, altarPos,
						isRareOffer(offerStack) ? SOUND_RARE_ITEM : SOUND_COMMON_ITEM,
						SoundSource.BLOCKS, 1.0f, 1.0f
				);
				debug(player, "Offer spawned: " + offerStack.getHoverName().getString());
			} else {
				debug(player, "Offer NOT spawned (empty or blocked)");
			}
		}

		if (profile.disableAltarUntilNight()) {
			DamnationAltarSacrificeRouter.disableAltarUntilNextNight(level, altarPos);
			debug(player, "Pink sheep: altar disabled until next night");
		}

		if (!damageCurse && !blightCurse) {
			int delta = 2 + random.nextInt(2);
			int rel = setRelationClamped(player, getOrInitRelation(player) + delta);
			debug(player, "Sheep offering " + profile.color().getName() + ". Relation=" + rel + " (+" + delta + ")");
		} else {
			debug(player, "Sheep offering " + profile.color().getName() + ". Relation unchanged because curse triggered");
		}
	}

	private static void handleWrongSacrifice(ServerLevel level, ServerPlayer player, BlockPos altarPos) {
		spawnSacrificeFailedVfx(level, altarPos);
		applyAltarsCurse(player);

		int delta = -(4 + level.getRandom().nextInt(7));
		int rel = setRelationClamped(player, getOrInitRelation(player) + delta);
		debug(player, "Wrong sacrifice: altar curse. Relation=" + rel + " (" + delta + ")");
	}

	private static int tryInfectCorrectBlightNearAltar(ServerLevel level, BlockPos altarPos) {
		List<BlockPos> candidates = new ArrayList<>();
		int radiusSqr = CORRECT_BLIGHT_RADIUS * CORRECT_BLIGHT_RADIUS;
		for (int dx = -CORRECT_BLIGHT_RADIUS; dx <= CORRECT_BLIGHT_RADIUS; dx++) {
			for (int dz = -CORRECT_BLIGHT_RADIUS; dz <= CORRECT_BLIGHT_RADIUS; dz++) {
				if (dx * dx + dz * dz > radiusSqr) continue;
				for (int dy = -CORRECT_BLIGHT_VERTICAL_RADIUS; dy <= CORRECT_BLIGHT_VERTICAL_RADIUS; dy++) {
					BlockPos candidate = altarPos.offset(dx, dy, dz);
					if (!level.hasChunkAt(candidate)) continue;
					BlockState state = level.getBlockState(candidate);
					if (BlightSpreadHelper.canBeBlighted(state)) {
						candidates.add(candidate.immutable());
					}
				}
			}
		}

		int targetCount = Mth.nextInt(level.getRandom(), CORRECT_BLIGHT_BLOCKS_MIN, CORRECT_BLIGHT_BLOCKS_MAX);
		int infected = 0;
		while (!candidates.isEmpty() && infected < targetCount) {
			BlockPos candidate = candidates.remove(level.getRandom().nextInt(candidates.size()));
			if (BlightSpreadHelper.infectTaggedBlock(level, candidate, altarPos.getY())) {
				infected++;
			}
		}
		return infected;
	}

	private static void damageRitualDagger(ServerPlayer player, ItemStack dagger) {
		if (dagger == null || dagger.isEmpty()) return;
		dagger.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
	}

	private static boolean isRitualDagger(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() == TimothatysTrinketsModItems.RITUAL_DAGGER.get();
	}

	private static void spawnBloodParticles(ServerLevel level, LivingEntity victim) {
		level.sendParticles(
				TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
				victim.getX(), victim.getY() + 0.6D, victim.getZ(),
				43,
				0.28D, 0.22D, 0.28D,
				0.03D
		);
	}

	private static void spawnSacrificeSuccessVfx(ServerLevel level, BlockPos altarPos) {
		level.sendParticles(
				TimothatysTrinketsModParticleTypes.SACRIFICE_SUCCES.get(),
				altarPos.getX() + 0.5D,
				altarPos.getY() + 0.05D,
				altarPos.getZ() + 0.5D,
				1,
				0.0D, 0.0D, 0.0D,
				0.0D
		);
	}

	private static void spawnSacrificeFailedVfx(ServerLevel level, BlockPos altarPos) {
		level.sendParticles(
				TimothatysTrinketsModParticleTypes.SACRIFICE_FAILED.get(),
				altarPos.getX() + 0.5D,
				altarPos.getY() + 0.05D,
				altarPos.getZ() + 0.5D,
				1,
				0.0D, 0.0D, 0.0D,
				0.0D
		);
	}

	private static void applyAltarsCurse(ServerPlayer player) {
		MobEffectInstance current = player.getEffect(TimothatysTrinketsModMobEffects.ALTARS_CURSE);
		int nextAmplifier = current == null ? 0 : Math.min(ALTARS_CURSE_MAX_LEVEL - 1, current.getAmplifier() + 1);
		int duration = current == null ? ALTARS_CURSE_DURATION_TICKS : Math.max(current.getDuration(), ALTARS_CURSE_DURATION_TICKS);
		player.addEffect(new MobEffectInstance(
				TimothatysTrinketsModMobEffects.ALTARS_CURSE,
				duration,
				nextAmplifier,
				false,
				true,
				true
		));
	}

	private static boolean rollChance(RandomSource random, float chance) {
		return random.nextFloat() < Mth.clamp(chance, 0.0F, 1.0F);
	}

	private static int getOrInitRelation(ServerPlayer player) {
		return DamnationAltarRelationHandler.getOrInitRelation(player);
	}

	private static int setRelationClamped(ServerPlayer player, int value) {
		int clamped = Mth.clamp(value, 0, 100);
		DamnationAltarRelationHandler.setRelation(player, clamped);
		return clamped;
	}

	private static boolean isRareOffer(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		return stack.is(RARE_ITEM_TAG);
	}

	private static void playAtAltar(ServerLevel level, BlockPos altarPos, SoundEvent sound, SoundSource source, float volume, float pitch) {
		level.playSound(
				null,
				altarPos.getX() + 0.5D,
				altarPos.getY() + 0.5D,
				altarPos.getZ() + 0.5D,
				sound,
				source,
				volume,
				pitch
		);
	}

	private static void debug(ServerPlayer player, String message) {
		TimothatysTrinketsDebug.altar(player, message);
	}

	private enum SheepOfferingProfile {
		WHITE(DyeColor.WHITE, 0.0F, 0.0F, false, false, 0.0F),
		BLACK(DyeColor.BLACK, 0.08F, 0.08F, false, false, 0.0F),
		RED(DyeColor.RED, 0.05F, 0.05F, false, false, 0.03F),
		GRAY(DyeColor.GRAY, 0.0F, 0.0F, true, false, 0.0F),
		PINK(DyeColor.PINK, 0.0F, 0.0F, false, true, 0.0F);

		private final DyeColor color;
		private final float damageCurseBonus;
		private final float blightCurseBonus;
		private final boolean spawnNecroSummons;
		private final boolean disableAltarUntilNight;
		private final float strengthChance;

		SheepOfferingProfile(DyeColor color, float damageCurseBonus, float blightCurseBonus,
				boolean spawnNecroSummons, boolean disableAltarUntilNight, float strengthChance) {
			this.color = color;
			this.damageCurseBonus = damageCurseBonus;
			this.blightCurseBonus = blightCurseBonus;
			this.spawnNecroSummons = spawnNecroSummons;
			this.disableAltarUntilNight = disableAltarUntilNight;
			this.strengthChance = strengthChance;
		}

		static SheepOfferingProfile fromSheep(Sheep sheep) {
			for (SheepOfferingProfile profile : values()) {
				if (profile.color == sheep.getColor()) {
					return profile;
				}
			}
			return null;
		}

		DyeColor color() {
			return color;
		}

		float damageCurseBonus() {
			return damageCurseBonus;
		}

		float blightCurseBonus() {
			return blightCurseBonus;
		}

		boolean spawnNecroSummons() {
			return spawnNecroSummons;
		}

		boolean forceOffer() {
			return disableAltarUntilNight;
		}

		boolean disableAltarUntilNight() {
			return disableAltarUntilNight;
		}

		float strengthChance() {
			return strengthChance;
		}
	}
}
