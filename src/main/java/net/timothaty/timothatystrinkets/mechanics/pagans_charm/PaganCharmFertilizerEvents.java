package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.item.PagansCharmItem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PaganCharmFertilizerEvents {
	private PaganCharmFertilizerEvents() {
	}

	@SubscribeEvent
	public static void onBonemeal(BonemealEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;

		Player player = event.getPlayer();
		if (player == null)
			return;

		ItemStack charm = PaganCharmCharge.findEquippedCharm(player);
		if (charm.isEmpty())
			return;

		ItemStack stack = event.getStack();
		if (stack.isEmpty() || !stack.is(Items.BONE_MEAL) || !event.isValidBonemealTarget())
			return;

		BlockPos targetPos = event.getPos();
		CropAge targetAge = CropAge.from(level, targetPos, event.getState(), true);
		if (targetAge == null || targetAge.isMaxAge())
			return;
		if (level.random.nextFloat() >= PaganCharmTuning.FERTILIZER_PROC_CHANCE)
			return;
		if (!spendFertilizerCharge(charm))
			return;

		targetAge.setAge(level, targetPos, targetAge.maxAge());
		fertilizeNeighborCrops(level, targetPos, charm);
		spawnBiomeEnergy(level, targetPos);

		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}

		event.setSuccessful(true);
	}

	private static void fertilizeNeighborCrops(ServerLevel level, BlockPos center, ItemStack charm) {
		int radius = PaganCharmTuning.FERTILIZER_RADIUS;
		int verticalRadius = PaganCharmTuning.FERTILIZER_VERTICAL_RADIUS;

		for (int y = -verticalRadius; y <= verticalRadius; y++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (x == 0 && y == 0 && z == 0)
						continue;

					BlockPos cropPos = center.offset(x, y, z);
					if (level.random.nextFloat() >= PaganCharmTuning.FERTILIZER_PROC_CHANCE)
						continue;

					CropAge cropAge = CropAge.from(level, cropPos, level.getBlockState(cropPos), false);
					if (cropAge == null || cropAge.isMaxAge())
						continue;
					if (!spendFertilizerCharge(charm))
						return;

					cropAge.setAge(level, cropPos, cropAge.age() + 1);
					spawnRunicFertilizer(level, cropPos);
				}
			}
		}
	}

	private static boolean spendFertilizerCharge(ItemStack charm) {
		int charge = PagansCharmItem.getCharge(charm);
		if (charge < PaganCharmTuning.FERTILIZER_PROC_CHARGE_COST)
			return false;

		PagansCharmItem.setCharge(charm, charge - PaganCharmTuning.FERTILIZER_PROC_CHARGE_COST);
		return true;
	}

	private static void spawnRunicFertilizer(ServerLevel level, BlockPos cropPos) {
		for (int i = 0; i < PaganCharmTuning.FERTILIZER_RUNE_PARTICLES; i++) {
			level.sendParticles(
					TimothatysTrinketsModParticleTypes.RUNIC_FERTILIZER.get(),
					cropPos.getX() + 0.5D,
					cropPos.getY() + 0.018D,
					cropPos.getZ() + 0.5D,
					0,
					0.0D,
					0.0D,
					0.0D,
					0.0D
			);
		}
	}

	private static void spawnBiomeEnergy(ServerLevel level, BlockPos cropPos) {
		int color = level.getBiome(cropPos).value().getGrassColor(cropPos.getX(), cropPos.getZ());
		double red = ((color >> 16) & 255) / 255.0D;
		double green = ((color >> 8) & 255) / 255.0D;
		double blue = (color & 255) / 255.0D;

		for (int i = 0; i < PaganCharmTuning.FERTILIZER_BIOME_ENERGY_PARTICLES; i++) {
			double x = cropPos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.48D;
			double y = cropPos.getY() + 0.18D + level.random.nextDouble() * 0.42D;
			double z = cropPos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.48D;
			level.sendParticles(TimothatysTrinketsModParticleTypes.BIOME_ENERGY.get(), x, y, z, 0, red, green, blue, 1.0D);
		}
	}

	private record CropAge(BlockState state, IntegerProperty property, int age, int maxAge) {
		private static CropAge from(ServerLevel level, BlockPos pos, BlockState state, boolean requireBonemealable) {
			if (!level.getBlockState(pos.below()).is(Blocks.FARMLAND))
				return null;
			if (requireBonemealable && !(state.getBlock() instanceof BonemealableBlock))
				return null;

			IntegerProperty ageProperty = findAgeProperty(state);
			if (ageProperty == null)
				return null;

			int age = state.getValue(ageProperty);
			int maxAge = getMaxAge(ageProperty);
			return new CropAge(state, ageProperty, age, maxAge);
		}

		private boolean isMaxAge() {
			return this.age >= this.maxAge;
		}

		private void setAge(ServerLevel level, BlockPos pos, int newAge) {
			int clampedAge = Math.min(this.maxAge, Math.max(0, newAge));
			level.setBlock(pos, this.state.setValue(this.property, clampedAge), 2);
		}

		private static IntegerProperty findAgeProperty(BlockState state) {
			for (Property<?> property : state.getProperties()) {
				if (property instanceof IntegerProperty integerProperty && "age".equals(integerProperty.getName())) {
					return integerProperty;
				}
			}
			return null;
		}

		private static int getMaxAge(IntegerProperty property) {
			int maxAge = 0;
			for (Integer value : property.getPossibleValues()) {
				if (value != null && value > maxAge) {
					maxAge = value;
				}
			}
			return maxAge;
		}
	}
}
