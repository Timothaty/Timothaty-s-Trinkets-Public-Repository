package net.timothaty.timothatystrinkets.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public final class TimothatysTrinketsCriteriaTriggers {
	public static final DeferredRegister<CriterionTrigger<?>> REGISTRY = DeferredRegister.create(
			BuiltInRegistries.TRIGGER_TYPES,
			TimothatysTrinketsMod.MODID
	);

	private static final DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> SACRIFICE_SHEEP = register("sacrifice_sheep");
	private static final DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> BLOOD_BOILING_SACRIFICE = register("blood_boiling_sacrifice");
	private static final DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> ABSORB_SOUL_ORB_WITH_DUALITY = register("absorb_soul_orb_with_duality");
	private static final DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> USE_CLEANSING_DUST = register("use_cleansing_dust");
	private static final DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> STEP_ON_BLIGHT = register("step_on_blight");
	private static final DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> GIVE_INDULGENCY_TO_CLERIC = register("give_indulgency_to_cleric");

	private TimothatysTrinketsCriteriaTriggers() {
	}

	public static void triggerSacrificeSheep(ServerPlayer player) {
		SACRIFICE_SHEEP.get().trigger(player);
	}

	public static void triggerBloodBoilingSacrifice(ServerPlayer player) {
		BLOOD_BOILING_SACRIFICE.get().trigger(player);
	}

	public static void triggerAbsorbSoulOrbWithDuality(ServerPlayer player) {
		ABSORB_SOUL_ORB_WITH_DUALITY.get().trigger(player);
	}

	public static void triggerUseCleansingDust(ServerPlayer player) {
		USE_CLEANSING_DUST.get().trigger(player);
	}

	public static void triggerStepOnBlight(ServerPlayer player) {
		STEP_ON_BLIGHT.get().trigger(player);
	}

	public static void triggerGiveIndulgencyToCleric(ServerPlayer player) {
		GIVE_INDULGENCY_TO_CLERIC.get().trigger(player);
	}

	private static DeferredHolder<CriterionTrigger<?>, PlayerActionTrigger> register(String id) {
		return REGISTRY.register(id, PlayerActionTrigger::new);
	}

	public static final class PlayerActionTrigger extends SimpleCriterionTrigger<PlayerActionInstance> {
		@Override
		public Codec<PlayerActionInstance> codec() {
			return PlayerActionInstance.CODEC;
		}

		public void trigger(ServerPlayer player) {
			this.trigger(player, ignored -> true);
		}
	}

	public record PlayerActionInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<PlayerActionInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerActionInstance::player)
		).apply(instance, PlayerActionInstance::new));
	}
}
