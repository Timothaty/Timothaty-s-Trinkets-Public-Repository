package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.util.VampiricFangsCurios;
import net.timothaty.timothatystrinkets.util.VampiricFangsData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BottleOfBloodItem extends Item {
	private static final int USE_DURATION = 32;
	private static final int NAUSEA_TICKS = 20 * 15;
	private static final int NAUSEA_AMPLIFIER = 1;
	private static final double FAMES_GAIN = 5.0D;
	private static final SoundEvent BLOOD_DRINK_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "bottle_of_blood_drink"));
	private static final double MODEL_UNIT = 1.0D / 16.0D;
	private static final double STANDING_EYE_MODEL_Y = 25.92D;
	private static final double MOUTH_MODEL_X = -0.5D * MODEL_UNIT;
	private static final double MOUTH_EYE_Y_OFFSET = (24.8D - STANDING_EYE_MODEL_Y) * MODEL_UNIT;
	private static final double MOUTH_FORWARD_OFFSET = 5.0D * MODEL_UNIT;
	private static final int MOUTH_BLOOD_BITS_PER_DRINK_SOUND = 4;
	private static final int MOUTH_BLOOD_BITS_ON_FINISH = 9;

	public BottleOfBloodItem() {
		super(new Item.Properties().stacksTo(16));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public SoundEvent getDrinkingSound() {
		return BLOOD_DRINK_SOUND;
	}

	@Override
	public SoundEvent getEatingSound() {
		return BLOOD_DRINK_SOUND;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		if (!(entity instanceof Player player) || !(level instanceof ServerLevel serverLevel))
			return;
		if (!VampiricFangsCurios.hasEquippedFangs(player))
			return;
		if (shouldSpawnDrinkUseParticles(stack, player, remainingUseDuration)) {
			spawnMouthBloodParticles(serverLevel, player, MOUTH_BLOOD_BITS_PER_DRINK_SOUND);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return stack;
		}

		if (!level.isClientSide()) {
			ItemStack equippedFangs = VampiricFangsCurios.getEquippedFangs(player);
			if (equippedFangs.isEmpty()) {
				player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, NAUSEA_AMPLIFIER, false, true, true));
			} else {
				if (level instanceof ServerLevel serverLevel) {
					spawnMouthBloodParticles(serverLevel, player, MOUTH_BLOOD_BITS_ON_FINISH);
				}
				VampiricFangsData.addFames(equippedFangs, FAMES_GAIN);
			}
		}

		if (player.getAbilities().instabuild) {
			return stack;
		}

		ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
		if (stack.getCount() <= 1) {
			return emptyBottle;
		}

		stack.shrink(1);
		if (!player.getInventory().add(emptyBottle)) {
			player.drop(emptyBottle, false);
		}
		return stack;
	}

	private static boolean shouldSpawnDrinkUseParticles(ItemStack stack, LivingEntity entity, int remainingUseDuration) {
		int useDuration = stack.getUseDuration(entity);
		int ticksUsed = useDuration - remainingUseDuration;
		int warmupTicks = (int) ((float) useDuration * 0.21875F);
		return ticksUsed > warmupTicks && remainingUseDuration % 4 == 0;
	}

	private static void spawnMouthBloodParticles(ServerLevel serverLevel, Player player, int count) {
		Vec3 forward = player.getViewVector(1.0F);
		if (forward.lengthSqr() < 1.0E-6D) {
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			forward = forward.normalize();
		}

		double yawRadians = Math.toRadians(player.getYRot());
		Vec3 right = new Vec3(-Math.cos(yawRadians), 0.0D, -Math.sin(yawRadians));
		Vec3 up = right.cross(forward);
		if (up.lengthSqr() < 1.0E-6D) {
			up = new Vec3(0.0D, 1.0D, 0.0D);
		} else {
			up = up.normalize();
		}

		Vec3 mouth = player.getEyePosition(1.0F)
				.add(right.scale(MOUTH_MODEL_X))
				.add(up.scale(MOUTH_EYE_Y_OFFSET))
				.add(forward.scale(MOUTH_FORWARD_OFFSET));
		RandomSource random = player.getRandom();

		for (int i = 0; i < count; i++) {
			Vec3 position = mouth
					.add(right.scale((random.nextDouble() - 0.5D) * 0.035D))
					.add(up.scale((random.nextDouble() - 0.5D) * 0.03D));
			Vec3 velocity = forward.scale(0.015D + random.nextDouble() * 0.025D)
					.add(right.scale((random.nextDouble() - 0.5D) * 0.02D))
					.add(up.scale(-0.010D - random.nextDouble() * 0.025D));
			serverLevel.sendParticles(
					TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
					position.x,
					position.y,
					position.z,
					0,
					velocity.x,
					velocity.y,
					velocity.z,
					1.0D
			);
		}
	}
}
