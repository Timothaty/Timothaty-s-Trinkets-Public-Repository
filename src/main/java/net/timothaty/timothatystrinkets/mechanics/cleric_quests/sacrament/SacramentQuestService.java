package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestActionBarScheduler;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestCeremonyService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestDialogue;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestEffects;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestProgress;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestRewardService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestSavedData;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

public final class SacramentQuestService {
	public static final int FASTING_REQUIRED_SECONDS = 600;
	private static final int DESERT_GRACE_SECONDS = 5;

	private SacramentQuestService() {
	}

	public static boolean offer(ServerLevel level, Villager cleric, ServerPlayer player, SacramentOfferingType type) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.getOrCreate(player.getUUID());
		if (!progress.humilityCompleted() || progress.sacramentCompleted())
			return false;
		if (progress.sacramentStage() == SacramentStage.NONE)
			progress.beginSacrament(cleric.getUUID(), level.dimension());
		if (progress.sacramentStage() != SacramentStage.OFFERINGS || !isBoundTo(progress, level, cleric))
			return false;
		if ((progress.sacramentOfferingMask() & type.bit()) != 0)
			return false;
		progress.setSacramentOfferingMask(progress.sacramentOfferingMask() | type.bit());
		data.changed(player.getUUID());
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cleric.getX(), cleric.getY() + cleric.getBbHeight() * 0.65D, cleric.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.03D);
		level.playSound(null, cleric.blockPosition(), SoundEvents.DYE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.sacrament.offer_received"), true);
		if (hasAllOfferings(progress))
			beginOfferingsCeremony(level, cleric, player);
		return true;
	}

	public static boolean retryOfferingsCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestProgress progress = ClericQuestSavedData.get(level).get(player.getUUID());
		return progress != null && progress.sacramentStage() == SacramentStage.OFFERINGS && hasAllOfferings(progress)
			&& isBoundTo(progress, level, cleric) && beginOfferingsCeremony(level, cleric, player);
	}

	private static boolean beginOfferingsCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		return ClericQuestCeremonyService.begin(level, cleric, player, ClericQuestCeremonyService.CeremonyKind.SACRAMENT_OFFERINGS);
	}

	public static void finishOfferingsCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.OFFERINGS || !hasAllOfferings(progress) || !isBoundTo(progress, level, cleric))
			return;
		List<ResourceLocation> targets = SacramentTargetSelector.selectThree(level);
		if (targets.size() != 3)
			return;
		progress.setSacramentTargets(targets);
		progress.setSacramentKilledMask(0);
		progress.setSacramentStage(SacramentStage.HUNT_ACTIVE);
		data.changed(player.getUUID());
		ClericQuestEffects.playStageSound(player, 1.0F);
		ClericQuestDialogue.show(cleric, player, "dialogue.timothatys_trinkets.cleric.sacrament.hunt_started");
	}

	public static boolean recordHuntKill(ServerPlayer player, ResourceLocation entityTypeId) {
		ClericQuestSavedData data = ClericQuestSavedData.get(player.serverLevel());
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.HUNT_ACTIVE)
			return false;
		int targetIndex = progress.sacramentTargets().indexOf(entityTypeId);
		if (targetIndex < 0)
			return false;
		int bit = 1 << targetIndex;
		if ((progress.sacramentKilledMask() & bit) != 0)
			return false;
		progress.setSacramentKilledMask(progress.sacramentKilledMask() | bit);
		int completed = Integer.bitCount(progress.sacramentKilledMask());
		if (completed >= 3)
			progress.setSacramentStage(SacramentStage.HUNT_RETURN);
		data.changed(player.getUUID());
		ClericQuestEffects.playSacramentMobSlayed(player);
		ClericQuestEffects.confirmation(player);
		ClericQuestActionBarScheduler.show(player, Component.translatable("message.timothatys_trinkets.cleric_quest.sacrament.hunt_progress", completed));
		return true;
	}

	public static boolean beginHuntReturnCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestProgress progress = ClericQuestSavedData.get(level).get(player.getUUID());
		return progress != null && progress.sacramentStage() == SacramentStage.HUNT_RETURN && isBoundTo(progress, level, cleric)
			&& ClericQuestCeremonyService.begin(level, cleric, player, ClericQuestCeremonyService.CeremonyKind.SACRAMENT_HUNT_RETURN);
	}

	public static void finishHuntReturnCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.HUNT_RETURN || !isBoundTo(progress, level, cleric))
			return;
		progress.setSacramentStage(SacramentStage.FAST_ACTIVE);
		progress.setFastingSeconds(0);
		progress.setFastingHasStarted(false);
		progress.setDesertExitGraceSeconds(0);
		data.changed(player.getUUID());
		ClericQuestEffects.playStageSound(player, 2.0F);
		ClericQuestDialogue.show(cleric, player, "dialogue.timothatys_trinkets.cleric.sacrament.fast_started");
	}

	public static boolean beginRestartCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestProgress progress = ClericQuestSavedData.get(level).get(player.getUUID());
		return progress != null && progress.sacramentStage() == SacramentStage.RESTART_REQUIRED && isBoundTo(progress, level, cleric)
			&& progress.sacramentTargets().size() == 3
			&& ClericQuestCeremonyService.begin(level, cleric, player, ClericQuestCeremonyService.CeremonyKind.SACRAMENT_RESTART);
	}

	public static void finishRestartCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.RESTART_REQUIRED || !isBoundTo(progress, level, cleric) || progress.sacramentTargets().size() != 3)
			return;
		progress.setSacramentKilledMask(0);
		progress.setFastingSeconds(0);
		progress.setFastingHasStarted(false);
		progress.setDesertExitGraceSeconds(0);
		progress.setSacramentStage(SacramentStage.HUNT_ACTIVE);
		data.changed(player.getUUID());
		ClericQuestDialogue.show(cleric, player, "dialogue.timothatys_trinkets.cleric.sacrament.hunt_restarted");
	}

	public static boolean beginRewardCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestProgress progress = ClericQuestSavedData.get(level).get(player.getUUID());
		return progress != null && progress.sacramentStage() == SacramentStage.FAST_RETURN && isBoundTo(progress, level, cleric)
			&& ClericQuestCeremonyService.begin(level, cleric, player, ClericQuestCeremonyService.CeremonyKind.SACRAMENT_REWARD);
	}

	public static void finishRewardCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.FAST_RETURN || !isBoundTo(progress, level, cleric))
			return;
		if (!ClericQuestRewardService.grantSacrament(level, cleric, player))
			return;
		progress.completeSacrament();
		data.changed(player.getUUID());
	}

	public static void tickFastingSecond(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.FAST_ACTIVE)
			return;
		if (level.getBiome(player.blockPosition()).is(net.neoforged.neoforge.common.Tags.Biomes.IS_DESERT)) {
			if (!progress.fastingHasStarted())
				progress.setFastingHasStarted(true);
			if (progress.desertExitGraceSeconds() != 0)
				progress.setDesertExitGraceSeconds(0);
			progress.setFastingSeconds(progress.fastingSeconds() + 1);
			if (progress.fastingSeconds() >= FASTING_REQUIRED_SECONDS) {
				completeFast(player, progress);
				data.changed(player.getUUID());
				return;
			}
			data.changed(player.getUUID());
			return;
		}
		if (!progress.fastingHasStarted())
			return;
		int grace = progress.desertExitGraceSeconds() + 1;
		if (grace >= DESERT_GRACE_SECONDS) {
			progress.setFastingSeconds(0);
			progress.setFastingHasStarted(false);
			progress.setDesertExitGraceSeconds(0);
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.sacrament.fast_broken_left_desert"), true);
		} else {
			progress.setDesertExitGraceSeconds(grace);
		}
		data.changed(player.getUUID());
	}

	public static void breakFastWithFood(ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(player.serverLevel());
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.FAST_ACTIVE || !progress.fastingHasStarted())
			return;
		progress.setFastingSeconds(0);
		progress.setFastingHasStarted(false);
		progress.setDesertExitGraceSeconds(0);
		data.changed(player.getUUID());
		player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.sacrament.fast_broken_food"), true);
	}

	private static void completeFast(ServerPlayer player, ClericQuestProgress progress) {
		progress.setFastingSeconds(FASTING_REQUIRED_SECONDS);
		progress.setFastingHasStarted(false);
		progress.setDesertExitGraceSeconds(0);
		progress.setSacramentStage(SacramentStage.FAST_RETURN);
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(20.0F);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60 * 5, 1, false, true, true));
		ClericQuestEffects.majorCompletion(player);
		ClericQuestEffects.playStageSound(player, 2.0F);
		ClericQuestActionBarScheduler.show(player, Component.translatable("message.timothatys_trinkets.cleric_quest.sacrament.fast_completed"));
	}

	public static boolean isBoundTo(ClericQuestProgress progress, ServerLevel level, Villager cleric) {
		return progress.sacramentClericId() != null
			&& progress.sacramentClericId().equals(cleric.getUUID())
			&& progress.sacramentClericDimension() != null
			&& progress.sacramentClericDimension().equals(level.dimension());
	}

	public static boolean hasAllOfferings(ClericQuestProgress progress) {
		return (progress.sacramentOfferingMask() & SacramentOfferingType.ALL_MASK) == SacramentOfferingType.ALL_MASK;
	}
}
