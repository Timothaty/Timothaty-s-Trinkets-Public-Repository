package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

public class GoldenHoneyCombItem extends Item {
	public static final int BASE_MAX_CHARGE = 15;
	public static final int HONEY_RECEPTACLE_CHARGE_PER_LEVEL = 3;
	public static final int MAX_HONEY_RECEPTACLE_LEVEL = 3;
	public static final int MAX_POSSIBLE_CHARGE = BASE_MAX_CHARGE + HONEY_RECEPTACLE_CHARGE_PER_LEVEL * MAX_HONEY_RECEPTACLE_LEVEL;

	private static final int USE_DURATION = 10;
	private static final int USE_COST = 3;
	private static final int HONEYCOMB_RECHARGE_AMOUNT = 2;
	private static final int USE_COOLDOWN = 20;

	private static final int BAR_COLOR = 0xFFD84A;

	private static final ResourceLocation HIVES_BOUNTY_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "hives_bounty");
	private static final ResourceLocation HONEY_RECEPTACLE_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "honey_receptacle");

	public GoldenHoneyCombItem() {
		super(new Item.Properties()
				.stacksTo(1)
				.durability(MAX_POSSIBLE_CHARGE));
	}

	public static int getHivesBountyLevel(ItemStack stack) {
		return getGoldenHoneyCombEnchantmentLevel(stack, HIVES_BOUNTY_ID);
	}

	public static int getHoneyReceptacleLevel(ItemStack stack) {
		return getGoldenHoneyCombEnchantmentLevel(stack, HONEY_RECEPTACLE_ID);
	}

	private static int getGoldenHoneyCombEnchantmentLevel(ItemStack stack, ResourceLocation enchantmentId) {
		if (stack.isEmpty()) {
			return 0;
		}

		for (Holder<Enchantment> enchantment : stack.getEnchantments().keySet()) {
			if (isEnchantment(enchantment, enchantmentId)) {
				return stack.getEnchantments().getLevel(enchantment);
			}
		}

		return 0;
	}

	private static boolean isGoldenHoneyCombEnchantment(Holder<Enchantment> enchantment) {
		return isEnchantment(enchantment, HIVES_BOUNTY_ID) || isEnchantment(enchantment, HONEY_RECEPTACLE_ID);
	}

	private static boolean isEnchantment(Holder<Enchantment> enchantment, ResourceLocation enchantmentId) {
		return enchantment.unwrapKey()
				.map(key -> key.location().equals(enchantmentId))
				.orElse(false);
	}

	public static int getMaxCharge(ItemStack stack) {
		int honeyReceptacleLevel = Math.max(0, Math.min(MAX_HONEY_RECEPTACLE_LEVEL, getHoneyReceptacleLevel(stack)));
		return BASE_MAX_CHARGE + HONEY_RECEPTACLE_CHARGE_PER_LEVEL * honeyReceptacleLevel;
	}

	public static int getCharge(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof GoldenHoneyCombItem)) {
			return 0;
		}

		int maxCharge = getMaxCharge(stack);
		return Math.max(0, Math.min(maxCharge, maxCharge - stack.getDamageValue()));
	}

	public static boolean isFullyCharged(ItemStack stack) {
		return getCharge(stack) >= getMaxCharge(stack);
	}

	public static void setCharge(ItemStack stack, int charge) {
		if (stack.isEmpty() || !(stack.getItem() instanceof GoldenHoneyCombItem)) {
			return;
		}

		int maxCharge = getMaxCharge(stack);
		int clampedCharge = Math.max(0, Math.min(maxCharge, charge));
		stack.setDamageValue(maxCharge - clampedCharge);
	}

	public static void consumeChargeSilently(ItemStack stack, int amount) {
		int currentCharge = getCharge(stack);
		int nextCharge = currentCharge - amount;

		if (nextCharge <= 0) {
			stack.shrink(1);
			return;
		}

		setCharge(stack, nextCharge);
	}

	@Override
	public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
		return isGoldenHoneyCombEnchantment(enchantment);
	}

	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		return isGoldenHoneyCombEnchantment(enchantment);
	}

	@Override
	public float getXpRepairRatio(ItemStack stack) {
		return 0.0F;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}

		if (getCharge(stack) <= 0) {
			return InteractionResultHolder.fail(stack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.EAT;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return stack;
		}

		if (!level.isClientSide()) {
			int chargeBeforeUse = getCharge(stack);

			if (chargeBeforeUse > 0) {
				player.heal(chargeBeforeUse * 0.25F);
				player.getCooldowns().addCooldown(this, USE_COOLDOWN);

				if (level instanceof ServerLevel serverLevel) {
					spawnEatingParticles(serverLevel, player);
				}

				consumeChargeSilently(stack, USE_COST);
			}
		}

		return stack;
	}

	private static void spawnEatingParticles(ServerLevel serverLevel, Player player) {
		serverLevel.sendParticles(
				ParticleTypes.WAX_ON,
				player.getX(),
				player.getY() + 0.9D,
				player.getZ(),
				10,
				0.35D,
				0.35D,
				0.35D,
				0.02D
		);
	}

	@Override
	public boolean overrideOtherStackedOnMe(
			ItemStack goldenComb,
			ItemStack otherStack,
			Slot slot,
			ClickAction action,
			Player player,
			SlotAccess carriedAccess
	) {
		if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) {
			return false;
		}

		if (!otherStack.is(Items.HONEYCOMB)) {
			return false;
		}

		int currentCharge = getCharge(goldenComb);
		int maxCharge = getMaxCharge(goldenComb);

		if (currentCharge >= maxCharge) {
			return true;
		}

		setCharge(goldenComb, currentCharge + HONEYCOMB_RECHARGE_AMOUNT);
		otherStack.shrink(1);

		player.level().playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				SoundEvents.HONEYCOMB_WAX_ON,
				SoundSource.PLAYERS,
				0.8F,
				1.2F
		);

		if (otherStack.isEmpty()) {
			carriedAccess.set(ItemStack.EMPTY);
		}

		slot.setChanged();
		return true;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int maxCharge = getMaxCharge(stack);
		if (maxCharge <= 0) {
			return 0;
		}

		return Math.round(13.0F * ((float) getCharge(stack) / (float) maxCharge));
	}
}
