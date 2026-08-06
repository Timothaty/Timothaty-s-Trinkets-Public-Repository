package net.timothaty.timothatystrinkets.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class MorgenshternItem extends Item {
	public static final int DURABILITY = 955;
	public static final double DISPLAYED_ATTACK_DAMAGE = 7.0D;
	public static final double DISPLAYED_ATTACK_SPEED = 0.75D;
	private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0D;
	private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

	public MorgenshternItem() {
		super(new Item.Properties()
				.durability(DURABILITY)
				.attributes(createAttributes()));
	}

	private static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
				.add(
						Attributes.ATTACK_DAMAGE,
						new AttributeModifier(
								BASE_ATTACK_DAMAGE_ID,
								DISPLAYED_ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE,
								AttributeModifier.Operation.ADD_VALUE
						),
						EquipmentSlotGroup.MAINHAND
				)
				.add(
						Attributes.ATTACK_SPEED,
						new AttributeModifier(
								BASE_ATTACK_SPEED_ID,
								DISPLAYED_ATTACK_SPEED - PLAYER_BASE_ATTACK_SPEED,
								AttributeModifier.Operation.ADD_VALUE
						),
						EquipmentSlotGroup.MAINHAND
				)
				.build();
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
		return repairCandidate.is(Items.IRON_INGOT);
	}

	@Override
	public int getEnchantmentValue() {
		return 14;
	}
}
