package net.timothaty.timothatystrinkets.mechanics.echo;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsAttributeHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class EchoSphereDeepDarkArmorHandler {
	private static final ResourceKey<Biome> DEEP_DARK_BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("deep_dark"));
	private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "echo_sphere_deep_dark_armor");
	private static final double ARMOR_BONUS = 3.0D;
	private static final long BIOME_CHECK_INTERVAL_TICKS = 20L;
	private static final long SAFETY_SYNC_INTERVAL_TICKS = 40L;
	private static final String NBT_DEEP_DARK_ARMOR_ACTIVE = "ttr_echo_deep_dark_armor_active";


	public static void onCurioEquip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.ECHO_SPHERE, true);
		updateDeepDarkArmor(player, true);
	}

	public static void onCurioUnequip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.ECHO_SPHERE, false);
		setDeepDarkArmorActive(player, false);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		long now = level.getGameTime();
		if ((now % SAFETY_SYNC_INTERVAL_TICKS) == 0L) {
			syncEquipState(player);
		}

		if (!TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.ECHO_SPHERE))
			return;
		if ((now % BIOME_CHECK_INTERVAL_TICKS) != 0L)
			return;

		updateDeepDarkArmor(player, false);
	}

	private static void syncEquipState(Player player) {
		boolean actual = TimothatysCuriosHelper.hasCurio(player, TimothatysTrinketsModItems.ECHO_SPHERE.get());
		boolean cached = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.ECHO_SPHERE);
		if (actual == cached)
			return;
		if (actual) {
			onCurioEquip(player, TimothatysCuriosHelper.findCurio(player, TimothatysTrinketsModItems.ECHO_SPHERE.get()));
		} else {
			onCurioUnequip(player, ItemStack.EMPTY);
		}
	}

	private static void updateDeepDarkArmor(Player player, boolean force) {
		boolean shouldHaveArmor = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.ECHO_SPHERE) && isInDeepDark(player);
		boolean active = player.getPersistentData().getBoolean(NBT_DEEP_DARK_ARMOR_ACTIVE);
		if (!force && shouldHaveArmor == active)
			return;
		setDeepDarkArmorActive(player, shouldHaveArmor);
	}

	private static void setDeepDarkArmorActive(Player player, boolean active) {
		if (active) {
			player.getPersistentData().putBoolean(NBT_DEEP_DARK_ARMOR_ACTIVE, true);
		} else {
			player.getPersistentData().remove(NBT_DEEP_DARK_ARMOR_ACTIVE);
		}

		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.ARMOR,
				ARMOR_MODIFIER_ID,
				ARMOR_BONUS,
				AttributeModifier.Operation.ADD_VALUE,
				active
		);
	}

	private static boolean isInDeepDark(Player player) {
		return player.level().getBiome(player.blockPosition()).is(DEEP_DARK_BIOME);
	}
}
