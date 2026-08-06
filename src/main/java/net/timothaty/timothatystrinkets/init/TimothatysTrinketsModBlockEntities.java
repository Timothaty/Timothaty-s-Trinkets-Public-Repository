package net.timothaty.timothatystrinkets.init;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.block.entity.DormantSphereBlockEntity;
import net.timothaty.timothatystrinkets.block.entity.EchoSphereBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TimothatysTrinketsModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimothatysTrinketsMod.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DamnationAltarBlockEntity>> DAMNATION_ALTAR = REGISTRY.register(
			"damnation_altar",
			() -> BlockEntityType.Builder.of(DamnationAltarBlockEntity::new, TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get()).build(null)
	);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoSphereBlockEntity>> ECHO_SPHERE = REGISTRY.register(
			"echo_sphere",
			() -> BlockEntityType.Builder.of(EchoSphereBlockEntity::new, TimothatysTrinketsModBlocks.ECHO_SPHERE.get()).build(null)
	);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DormantSphereBlockEntity>> DORMANT_SPHERE = REGISTRY.register(
			"dormant_sphere",
			() -> BlockEntityType.Builder.of(DormantSphereBlockEntity::new, TimothatysTrinketsModBlocks.DORMANT_SPHERE.get()).build(null)
	);

	private TimothatysTrinketsModBlockEntities() {
	}
}
