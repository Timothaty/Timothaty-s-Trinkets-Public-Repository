package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.salt_of_the_earth.SaltOfTheEarth;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsAttributeHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class HolyRosariumModifierService {
	private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"holy_rosarium_humility_ressurection_health"
	);
	private static final Map<Player, Integer> APPLIED_REVISIONS = Collections.synchronizedMap(new WeakHashMap<>());

	private HolyRosariumModifierService() {
	}

	public static void applyIfNeeded(Player player, HolyRosariumState state) {
		if (player == null || state == null || player.level().isClientSide())
			return;

		Integer appliedRevision = APPLIED_REVISIONS.get(player);
		if (appliedRevision != null && appliedRevision == state.revision())
			return;

		applyHealthModifier(player, state);
		APPLIED_REVISIONS.put(player, state.revision());
	}

	static void forget(Player player) {
		if (player != null)
			APPLIED_REVISIONS.remove(player);
	}

	private static void applyHealthModifier(Player player, HolyRosariumState state) {
		boolean shouldHaveBonus = state.hasCombination(HolyRosariumBead.HUMILITY, HolyRosariumBead.RESURRECTION);
		boolean hadBonus = player.getAttribute(Attributes.MAX_HEALTH) != null
				&& player.getAttribute(Attributes.MAX_HEALTH).getModifier(HEALTH_MODIFIER_ID) != null;
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.MAX_HEALTH,
				HEALTH_MODIFIER_ID,
				SaltOfTheEarth.MAX_HEALTH_BONUS,
				AttributeModifier.Operation.ADD_VALUE,
				shouldHaveBonus
		);
		if (!shouldHaveBonus && hadBonus && player.getHealth() > player.getMaxHealth())
			player.setHealth(player.getMaxHealth());
	}
}
