package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.item.GoldenHoneyCombItem;
import net.timothaty.timothatystrinkets.item.PagansCharmItem;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.FlamingEmberData;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.FlamingEmberEnvironment;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmBonuses;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmCharge;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmTuning;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteCurios;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteData;
import net.timothaty.timothatystrinkets.util.DrumsOfHasteStacks;
import net.timothaty.timothatystrinkets.util.VampiricFangsCurios;
import net.timothaty.timothatystrinkets.util.VampiricFangsData;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class TimothatysTrinketsSpecialTooltipLines {
	private TimothatysTrinketsSpecialTooltipLines() {}

	private static final int GOLDEN_HONEY_COMB_USE_COST = 3;
	private static final int PAGAN_CHARM_EMPTY_TEXT_COLOR = PaganCharmTuning.EMPTY_CHARGE_TEXT_COLOR;
	private static final int PAGAN_CHARM_FULL_TEXT_COLOR = PaganCharmTuning.FULL_CHARGE_TEXT_COLOR;
	private static final String PAGAN_CHARM_BASE_CHARGE_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pagans_charm.charge.0";
	private static final String PAGAN_CHARM_CAMPFIRE_CHARGE_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pagans_charm.charge.1";
	private static final String PAGAN_CHARM_MEDITATOR_CHARGE_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pagans_charm.charge.2";
	private static final String PAGAN_CHARM_FISHING_CHARGE_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pagans_charm.charge.3";
	private static final String PAGAN_CHARM_UNIQUE_BIOME_CHARGE_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pagans_charm.charge.4";
	private static final String FLAMING_EMBER_SHIFT_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".flaming_ember.shift";
	private static final String FLAMING_EMBER_BONUSES_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".flaming_ember.bonuses";
	private static final String FLAMING_EMBER_HEAT_SOURCE_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".flaming_ember.heat_source";
	private static final String FLAMING_EMBER_BIOME_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".flaming_ember.biome";
	private static final String FLAMING_EMBER_SUN_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".flaming_ember.sun";
	private static final String PACT_MEMBERS_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pact_of_alliance.members";
	private static final String PACT_SHIFT_KEY = "tooltip." + TimothatysTrinketsMod.MODID + ".pact_of_alliance.shift";

	private static final ResourceLocation FAMES_ICON_FONT = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "fames_icons");
	private static final Component FAMES_ICON = Component.literal("\uE001")
			.withStyle(style -> style.withFont(FAMES_ICON_FONT));

	private static final ResourceLocation DRUMS_ICON_FONT = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "drums_icons");
	private static final Component DRUMS_ICON = Component.literal("\uE002")
			.withStyle(style -> style.withFont(DRUMS_ICON_FONT));

	private static final ResourceLocation HONEYCOMB_ICON_FONT = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "honeycomb_icons");
	private static final Component HONEYCOMB_ICON = Component.literal("\uE003")
			.withStyle(style -> style.withFont(HONEYCOMB_ICON_FONT));

	private static final ResourceLocation PAGAN_CHARM_CHARGE_ICON_FONT = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "pc_charge_icons");
	private static final Component PAGAN_CHARM_CHARGE_ICON = Component.literal("\uE004")
			.withStyle(style -> style.withFont(PAGAN_CHARM_CHARGE_ICON_FONT));

	public static void addFor(ItemStack stack, List<Component> tooltip, Player player) {
		if (VampiricFangsCurios.isFangs(stack)) {
			tooltip.add(famesTooltipLine(stack));
		}
		if (DrumsOfHasteCurios.isDrumsStack(stack)) {
			tooltip.add(furyTooltipLine(stack));
		}
		if (stack.is(TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get())) {
			tooltip.add(goldenHoneyCombUsesTooltipLine(stack));
		}
		if (stack.is(TimothatysTrinketsModItems.PAGANS_CHARM.get())) {
			tooltip.add(paganCharmChargeTooltipLine(stack, player));
			addPaganCharmChargeBreakdown(tooltip, player);
		}
		if (stack.is(TimothatysTrinketsModItems.FLAMING_EMBER.get())) {
			addFlamingEmberLines(stack, tooltip, player);
		}
		if (PactOfAllianceHelper.isPactStack(stack)) {
			addPactOfAllianceLines(stack, tooltip);
		}
	}

	private static Component famesTooltipLine(ItemStack stack) {
		return Component.empty()
				.append(FAMES_ICON)
				.append(Component.literal(" Fames Sanguinis: " + VampiricFangsData.format(VampiricFangsData.getFames(stack)))
						.withStyle(ChatFormatting.DARK_RED));
	}

	private static Component furyTooltipLine(ItemStack stack) {
		return Component.empty()
				.append(DRUMS_ICON)
				.append(Component.literal(" Fury: " + DrumsOfHasteStacks.getStackFury(stack) + "/" + DrumsOfHasteData.MAX_STACKS)
						.withStyle(ChatFormatting.RED));
	}

	private static Component goldenHoneyCombUsesTooltipLine(ItemStack stack) {
		return Component.empty()
				.append(HONEYCOMB_ICON)
				.append(Component.literal(" Uses: " + getUses(stack) + "/" + getMaxUses(stack))
						.withStyle(ChatFormatting.GOLD));
	}

	private static Component paganCharmChargeTooltipLine(ItemStack stack, Player player) {
		double chargePerSecond = PaganCharmCharge.getPotentialChargePerSecond(player);
		return Component.empty()
				.append(PAGAN_CHARM_CHARGE_ICON)
				.append(Component.literal(" " + PagansCharmItem.getCharge(stack) + "/" + PagansCharmItem.getMaxCharge(stack))
						.withStyle(style -> style.withColor(getPaganCharmChargeTextColor(stack))))
				.append(Component.literal(" [+" + formatChargePerSecond(chargePerSecond) + "/s]")
						.withStyle(chargePerSecond > 0.0D ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
	}

	private static void addPactOfAllianceLines(ItemStack stack, List<Component> tooltip) {
		tooltip.add(Component.translatable(PACT_MEMBERS_KEY, PactOfAllianceHelper.getMemberCount(stack), PactOfAllianceHelper.MAX_MEMBERS)
				.withStyle(ChatFormatting.GOLD));

		if (!Screen.hasShiftDown()) {
			tooltip.add(Component.translatable(PACT_SHIFT_KEY).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		for (PactOfAllianceHelper.PactMember member : PactOfAllianceHelper.getMembers(stack)) {
			tooltip.add(Component.literal("- " + member.name()).withStyle(ChatFormatting.GRAY));
		}
	}

	private static void addPaganCharmChargeBreakdown(List<Component> tooltip, Player player) {
		if (!Screen.hasShiftDown())
			return;

		PaganCharmBonuses.ChargeBreakdown breakdown = PaganCharmCharge.getPotentialChargeBreakdown(player);
		addPaganCharmChargeBreakdownLine(tooltip, PAGAN_CHARM_BASE_CHARGE_KEY, breakdown.base());
		addPaganCharmChargeBreakdownLine(tooltip, PAGAN_CHARM_CAMPFIRE_CHARGE_KEY, breakdown.campfire());
		addPaganCharmChargeBreakdownLine(tooltip, PAGAN_CHARM_MEDITATOR_CHARGE_KEY, breakdown.otherMeditator());
		addPaganCharmChargeBreakdownLine(tooltip, PAGAN_CHARM_FISHING_CHARGE_KEY, breakdown.fishing());
		addPaganCharmChargeBreakdownLine(tooltip, PAGAN_CHARM_UNIQUE_BIOME_CHARGE_KEY, breakdown.uniqueBiome());
	}

	private static void addPaganCharmChargeBreakdownLine(List<Component> tooltip, String key, double amount) {
		if (amount <= 0.0D)
			return;

		tooltip.add(Component.literal("  ")
				.append(Component.translatable(key).withStyle(ChatFormatting.DARK_GREEN))
				.append(Component.literal(": +" + formatChargePerSecond(amount) + "/s").withStyle(ChatFormatting.GREEN)));
	}

	private static void addFlamingEmberLines(ItemStack stack, List<Component> tooltip, Player player) {
		if (!Screen.hasShiftDown()) {
			tooltip.add(Component.translatable(FLAMING_EMBER_SHIFT_KEY).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		FlamingEmberEnvironment.HeatBreakdown breakdown = FlamingEmberEnvironment.getPassiveHeatBreakdown(player);
		tooltip.add(Component.translatable(FLAMING_EMBER_BONUSES_KEY).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.literal("  " + formatHeat(FlamingEmberData.getHeat(stack)) + "/" + formatHeat(FlamingEmberData.MAX_HEAT) + ", ")
				.withStyle(ChatFormatting.GOLD)
				.append(formatSignedHeatPerSecond(breakdown.total())));
		tooltip.add(Component.literal("  ")
				.append(Component.translatable(FLAMING_EMBER_HEAT_SOURCE_KEY).withStyle(ChatFormatting.DARK_GREEN))
				.append(Component.literal(" " + breakdown.heatSources() + ", ").withStyle(ChatFormatting.GRAY))
				.append(formatSignedHeatPerSecond(breakdown.heatSource())));
		tooltip.add(Component.literal("  ")
				.append(Component.translatable(FLAMING_EMBER_BIOME_KEY).withStyle(ChatFormatting.DARK_GREEN))
				.append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
				.append(formatSignedHeatPerSecond(breakdown.biome())));
		tooltip.add(Component.literal("  ")
				.append(Component.translatable(FLAMING_EMBER_SUN_KEY).withStyle(ChatFormatting.DARK_GREEN))
				.append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
				.append(formatSignedHeatPerSecond(breakdown.sun())));
	}

	private static int getPaganCharmChargeTextColor(ItemStack stack) {
		int maxCharge = PagansCharmItem.getMaxCharge(stack);
		if (maxCharge <= 0)
			return PAGAN_CHARM_EMPTY_TEXT_COLOR;

		float progress = Math.max(0.0F, Math.min(1.0F, (float) PagansCharmItem.getCharge(stack) / (float) maxCharge));
		return lerpColor(PAGAN_CHARM_EMPTY_TEXT_COLOR, PAGAN_CHARM_FULL_TEXT_COLOR, progress);
	}

	private static int lerpColor(int from, int to, float progress) {
		int fromRed = (from >> 16) & 255;
		int fromGreen = (from >> 8) & 255;
		int fromBlue = from & 255;
		int toRed = (to >> 16) & 255;
		int toGreen = (to >> 8) & 255;
		int toBlue = to & 255;

		int red = Math.round(fromRed + (toRed - fromRed) * progress);
		int green = Math.round(fromGreen + (toGreen - fromGreen) * progress);
		int blue = Math.round(fromBlue + (toBlue - fromBlue) * progress);
		return (red << 16) | (green << 8) | blue;
	}

	private static String formatChargePerSecond(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.001D)
			return Integer.toString((int) Math.rint(value));

		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String formatHeat(double value) {
		return formatChargePerSecond(value);
	}

	private static Component formatSignedHeatPerSecond(double value) {
		ChatFormatting color = value > 0.0D ? ChatFormatting.GREEN : value < 0.0D ? ChatFormatting.RED : ChatFormatting.DARK_GRAY;
		String sign = value > 0.0D ? "+" : "";
		return Component.literal(sign + formatChargePerSecond(value) + "/s").withStyle(color);
	}

	private static int getUses(ItemStack stack) {
		return GoldenHoneyCombItem.getCharge(stack) / GOLDEN_HONEY_COMB_USE_COST;
	}

	private static int getMaxUses(ItemStack stack) {
		return GoldenHoneyCombItem.getMaxCharge(stack) / GOLDEN_HONEY_COMB_USE_COST;
	}
}
