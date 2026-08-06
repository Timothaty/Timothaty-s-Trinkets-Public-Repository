package net.timothaty.timothatystrinkets.init;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCurio;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumCurio;

import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.CuriosCapability;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class TimothatysTrinketsModCuriosCompat {
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_generic")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.FANGS.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new HolyRosariumCurio(stack), TimothatysTrinketsModItems.HOLY_ROSARIUM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new CorruptedRosariumCurio(stack), TimothatysTrinketsModItems.CORRUPTED_ROSARY.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_leather")).value(), 1, 1);
			}

		}, TimothatysTrinketsModItems.DRUMS_OF_HASTE.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}
		}, TimothatysTrinketsModItems.BELT_OF_OUTCAST.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("block.bamboo_wood.break")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.PAGANS_CHARM.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_undead_knight_armlet")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:farmers_ring_equip")).value(), 1, 1);
			}

		}, TimothatysTrinketsModItems.FARMERS_RING.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_undead_knight_armlet")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

		}, TimothatysTrinketsModItems.FLAMING_EMBER.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_ritual_dagger")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.RITUAL_DAGGER.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_void_sphere")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.VOID_SPHERE.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_echo_sphere")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.ECHO_SPHERE.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_fire_orb")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.FIRE_SPHERE.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:equip_venom_sphere")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.VENOM_SPHERE.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("timothatys_trinkets:striker_equip")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.STRIKER_OF_THE_MORNING_STAR.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_netherite")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.RUSTY_GAUNTLET.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_netherite")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.RUSTY_ARMLET.get());
		event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
			@Override
			public ItemStack getStack() {
				return stack;
			}

			@Override
			public SoundInfo getEquipSound(SlotContext slotContext) {
				return new SoundInfo(DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_netherite")).value(), 1, 1);
			}
		}, TimothatysTrinketsModItems.DUELISTS_GAUNTLET.get());
	}
}
