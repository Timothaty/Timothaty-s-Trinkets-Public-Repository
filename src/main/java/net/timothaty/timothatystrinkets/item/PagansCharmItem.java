package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmTuning;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class PagansCharmItem extends Item {
	public static final int MAX_CHARGE = PaganCharmTuning.MAX_CHARGE;
	public static final int CREATIVE_TAB_CHARGE = PaganCharmTuning.CREATIVE_TAB_CHARGE;
	private static final int BAR_COLOR = PaganCharmTuning.BAR_COLOR;

	public PagansCharmItem() {
		super(new Item.Properties()
				.stacksTo(1)
				.durability(MAX_CHARGE));
	}

	public static int getMaxCharge(ItemStack stack) {
		return stack.isEmpty() ? MAX_CHARGE : stack.getMaxDamage();
	}

	public static int getCharge(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof PagansCharmItem))
			return 0;

		int maxCharge = getMaxCharge(stack);
		return Math.max(0, Math.min(maxCharge, maxCharge - stack.getDamageValue()));
	}

	public static void setCharge(ItemStack stack, int charge) {
		if (stack.isEmpty() || !(stack.getItem() instanceof PagansCharmItem))
			return;

		int maxCharge = getMaxCharge(stack);
		int clampedCharge = Math.max(0, Math.min(maxCharge, charge));
		stack.setDamageValue(maxCharge - clampedCharge);
	}

	public static boolean addCharge(ItemStack stack, int amount) {
		if (amount <= 0)
			return false;

		int currentCharge = getCharge(stack);
		int maxCharge = getMaxCharge(stack);
		if (currentCharge >= maxCharge)
			return false;

		setCharge(stack, currentCharge + amount);
		return true;
	}

	@Override
	public ItemStack getDefaultInstance() {
		ItemStack stack = super.getDefaultInstance();
		setCharge(stack, CREATIVE_TAB_CHARGE);
		return stack;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

	@Override
	public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
		return false;
	}

	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		return false;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		return false;
	}

	@Override
	public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
		return 0;
	}

	@Override
	public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
		return ItemEnchantments.EMPTY;
	}

	@Override
	public float getXpRepairRatio(ItemStack stack) {
		return 0.0F;
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
		if (maxCharge <= 0)
			return 0;

		int charge = getCharge(stack);
		if (charge <= 0)
			return 0;

		return Math.max(1, Math.round(13.0F * ((float) charge / (float) maxCharge)));
	}
}
