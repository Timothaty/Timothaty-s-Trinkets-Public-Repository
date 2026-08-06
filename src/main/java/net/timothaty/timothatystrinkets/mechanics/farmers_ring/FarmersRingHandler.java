package net.timothaty.timothatystrinkets.mechanics.farmers_ring;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class FarmersRingHandler {

	private static final float TWIN_CHANCE = 0.1f;
	private static final float BONUS_CROP_CHANCE = 0.27f;
	private static final int EXTRA_MIN = 3;
	private static final int EXTRA_MAX = 5;

	private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY =
			EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

	private static final TagKey<EntityType<?>> FARMERS_RING_BREEDABLE =
			TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "farmers_ring_breedable"));

	private static final TagKey<Block> FARMERS_RING_CROPS =
			TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "farmers_ring_crops"));

	private static final TagKey<Item> FARMERS_RING_CROP_YIELDS =
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "farmers_ring_crop_yields"));

	@SubscribeEvent
	public static void onBabySpawn(BabyEntitySpawnEvent event) {
		if (event == null) return;

		Player player = event.getCausedByPlayer();
		if (player == null) return;

		Level level = player.level();
		if (level.isClientSide) return;

		if (!isFarmersRingEquippedCurios(player)) {
			log("BabySpawn: ring NOT equipped");
			return;
		}

		Mob parentA = event.getParentA();
		Mob parentB = event.getParentB();
		if (!(parentA instanceof Animal a) || !(parentB instanceof Animal b)) return;

		if (!a.getType().is(FARMERS_RING_BREEDABLE) || !b.getType().is(FARMERS_RING_BREEDABLE)) {
			log("BabySpawn: parents NOT in tag");
			return;
		}

		RandomSource random = player.getRandom();
		if (random.nextFloat() >= TWIN_CHANCE) {
			log("BabySpawn: chance failed");
			return;
		}

		if (!(level instanceof ServerLevel server)) return;

		AgeableMob extraChild = a.getBreedOffspring(server, b);
		if (extraChild == null) return;

		extraChild.setAge(-24000);

		double baseX = (a.getX() + b.getX()) * 0.5D;
		double baseY = Math.max(a.getY(), b.getY());
		double baseZ = (a.getZ() + b.getZ()) * 0.5D;

		boolean placed = false;
		for (int tries = 0; tries < 12; tries++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double dist = 0.9D + random.nextDouble() * 1.2D;
			double x = baseX + Math.cos(angle) * dist;
			double z = baseZ + Math.sin(angle) * dist;

			extraChild.moveTo(x, baseY, z, a.getYRot(), a.getXRot());

			if (server.noCollision(extraChild)) {
				placed = true;
				break;
			}
		}

		if (!placed) {
			double x = baseX + (random.nextDouble() - 0.5D) * 1.6D;
			double z = baseZ + (random.nextDouble() - 0.5D) * 1.6D;
			extraChild.moveTo(x, baseY, z, a.getYRot(), a.getXRot());
		}

		boolean added = server.addFreshEntity(extraChild);

		if (added) {
			double px = extraChild.getX();
			double py = extraChild.getY() + 0.6D;
			double pz = extraChild.getZ();

			server.sendParticles(ParticleTypes.HEART, px, py, pz, 6, 0.35D, 0.25D, 0.35D, 0.02D);
			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 10, 0.45D, 0.35D, 0.45D, 0.02D);

			server.playSound(null, extraChild.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.1F);
		}

		log("BabySpawn: addFreshEntity=" + added + " pos=" + extraChild.position());
	}

	@SubscribeEvent
	public static void onBlockDrops(BlockDropsEvent event) {
		if (event == null) return;
		if (!(event.getBreaker() instanceof Player player)) return;

		Level level = player.level();
		if (level.isClientSide) return;

		if (!isFarmersRingEquippedCurios(player)) return;

		BlockState state = event.getState();
		if (state == null) return;

		if (!state.is(FARMERS_RING_CROPS)) return;

		if (!isMatureCrop(state)) return;

		RandomSource random = player.getRandom();
		if (random.nextFloat() >= BONUS_CROP_CHANCE) return;

		int extra = EXTRA_MIN + random.nextInt((EXTRA_MAX - EXTRA_MIN) + 1);

		List<ItemEntity> drops = event.getDrops();
		if (drops == null || drops.isEmpty()) return;

		ItemEntity best = null;
		int bestCount = -1;

		for (ItemEntity it : drops) {
			if (it == null) continue;
			ItemStack st = it.getItem();
			if (st == null || st.isEmpty()) continue;

			if (!st.is(FARMERS_RING_CROP_YIELDS)) continue;

			int c = st.getCount();
			if (c > bestCount) {
				bestCount = c;
				best = it;
			}
		}

		if (best == null) return;

		ItemStack st = best.getItem();
		st.grow(extra);
		best.setItem(st);

		if (level instanceof ServerLevel server) {
			double px = best.getX();
			double py = best.getY() + 0.2D;
			double pz = best.getZ();

			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 12, 0.35D, 0.25D, 0.35D, 0.02D);
			server.playSound(null, best.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.9F, 1.0F);
		}
	}

	private static boolean isMatureCrop(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}

		IntegerProperty ageProp = null;
		for (Property<?> p : state.getProperties()) {
			if (p instanceof IntegerProperty ip && "age".equals(ip.getName())) {
				ageProp = ip;
				break;
			}
		}

		if (ageProp == null) return false;

		int current = state.getValue(ageProp);
		int max = current;
		for (Integer v : ageProp.getPossibleValues()) {
			if (v != null && v > max) max = v;
		}
		return current >= max;
	}

	private static boolean isFarmersRingEquippedCurios(Player player) {
		IItemHandler curios = player.getCapability(CURIOS_INVENTORY);
		if (curios == null) {
			log("Curios capability is NULL");
			return false;
		}

		Item ringItem = TimothatysTrinketsModItems.FARMERS_RING.get();

		for (int i = 0; i < curios.getSlots(); i++) {
			ItemStack st = curios.getStackInSlot(i);
			if (!st.isEmpty() && st.getItem() == ringItem) return true;
		}
		return false;
	}

	private static void log(String msg) {
		TimothatysTrinketsDebug.farmersRingLog(msg);
	}
}
