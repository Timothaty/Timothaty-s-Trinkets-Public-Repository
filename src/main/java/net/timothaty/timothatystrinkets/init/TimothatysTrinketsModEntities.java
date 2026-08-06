package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

import net.timothaty.timothatystrinkets.entity.*;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@EventBusSubscriber
public class TimothatysTrinketsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, TimothatysTrinketsMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<VFXIndulgencyBlessingEntity>> VFX_INDULGENCY_BLESSING = register("vfx_indulgency_blessing",
			EntityType.Builder.<VFXIndulgencyBlessingEntity>of(VFXIndulgencyBlessingEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<NecromancerEntity>> NECROMANCER = register("necromancer",
			EntityType.Builder.<NecromancerEntity>of(NecromancerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<DebtlordEntity>> DEBTLORD = register("debtlord",
			EntityType.Builder.<DebtlordEntity>of(DebtlordEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1.5f, 3.1f));
	public static final DeferredHolder<EntityType<?>, EntityType<DebtlordGroundDebrisEntity>> DEBTLORD_GROUND_DEBRIS = register("debtlord_ground_debris",
			EntityType.Builder.<DebtlordGroundDebrisEntity>of(DebtlordGroundDebrisEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<UndeadKnightEntity>> UNDEAD_KNIGHT = register("undead_knight",
			EntityType.Builder.<UndeadKnightEntity>of(UndeadKnightEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<SoulOrbEntity>> SOUL_ORB = register("soul_orb",
			EntityType.Builder.<SoulOrbEntity>of(SoulOrbEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.35f, 0.35f));
	public static final DeferredHolder<EntityType<?>, EntityType<TargetAreaEntity>> TARGET_AREA = register("target_area",
			EntityType.Builder.<TargetAreaEntity>of(TargetAreaEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).setTrackingRange(64).setUpdateInterval(2)

					.sized(1.0f, 0.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<BeatificPalliumEntity>> BEATIFIC_PALLIUM = register("beatific_pallium",
			EntityType.Builder.<BeatificPalliumEntity>of(BeatificPalliumEntity::new, MobCategory.MISC)
					.setShouldReceiveVelocityUpdates(false).setTrackingRange(64).setUpdateInterval(1)
					.sized(1.6F, 1.6F));
	public static final DeferredHolder<EntityType<?>, EntityType<CleansingRitualControllerEntity>> CLEANSING_RITUAL_CONTROLLER = register(
			"cleansing_ritual_controller",
			EntityType.Builder.<CleansingRitualControllerEntity>of(CleansingRitualControllerEntity::new, MobCategory.MISC)
					.setShouldReceiveVelocityUpdates(false).setTrackingRange(64).setUpdateInterval(1)
					.noSummon().fireImmune().sized(0.1F, 0.1F));
	public static final DeferredHolder<EntityType<?>, EntityType<CleansingDustManifestationEntity>> CLEANSING_DUST_MANIFESTATION = register(
			"cleansing_dust_manifestation",
			EntityType.Builder.<CleansingDustManifestationEntity>of(CleansingDustManifestationEntity::new, MobCategory.MISC)
					.setShouldReceiveVelocityUpdates(false).setTrackingRange(64).setUpdateInterval(1)
					.noSummon().fireImmune().sized(0.45F, 0.45F));

	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(RegisterSpawnPlacementsEvent event) {
		VFXIndulgencyBlessingEntity.init(event);
		NecromancerEntity.init(event);
		DebtlordEntity.init(event);
		DebtlordGroundDebrisEntity.init(event);
		UndeadKnightEntity.init(event);
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(VFX_INDULGENCY_BLESSING.get(), VFXIndulgencyBlessingEntity.createAttributes().build());
		event.put(NECROMANCER.get(), NecromancerEntity.createAttributes().build());
		event.put(DEBTLORD.get(), DebtlordEntity.createAttributes().build());
		event.put(DEBTLORD_GROUND_DEBRIS.get(), DebtlordGroundDebrisEntity.createAttributes().build());
		event.put(UNDEAD_KNIGHT.get(), UndeadKnightEntity.createAttributes().build());
	}
}
