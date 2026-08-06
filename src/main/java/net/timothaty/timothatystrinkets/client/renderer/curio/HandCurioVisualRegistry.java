package net.timothaty.timothatystrinkets.client.renderer.curio;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.model.curio.ChampionsGauntletModel;
import net.timothaty.timothatystrinkets.client.model.curio.DuelistsGauntletModel;
import net.timothaty.timothatystrinkets.client.model.curio.UndeadKnightsArmletModel;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;
import net.timothaty.timothatystrinkets.util.CuriosBraceletSlotHelper;
import net.timothaty.timothatystrinkets.util.CuriosHandsSlotHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class HandCurioVisualRegistry {
	public static final int CHAMPIONS_GAUNTLET_PRIORITY = 100;
	public static final int DUELISTS_GAUNTLET_PRIORITY = 200;
	public static final int ARM_ACCESSORY_PRIORITY = 100;

	private static final ResourceLocation CHAMPIONS_GAUNTLET_TEXTURE = texture("champions_gauntlet");
	private static final ResourceLocation DUELISTS_GAUNTLET_TEXTURE = texture("duelists_gauntlet");
	private static final ResourceLocation UNDEAD_KNIGHTS_ARMLET_TEXTURE = texture("undead_knights_armlet");
	private static final ResourceLocation UNDEAD_KNIGHTS_ARMLET_RIVETS_TEXTURE = texture("undead_knights_armlet_rivets");
	private static final Map<Item, HandCurioVisualDefinition> DEFINITIONS = new IdentityHashMap<>();
	private static boolean bootstrapped;

	private HandCurioVisualRegistry() {
	}

	public static synchronized void bootstrap() {
		if (bootstrapped)
			return;

		bootstrapped = true;
		register(new HandCurioVisualDefinition(
				TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get(),
				CuriosHandsSlotHelper.HANDS_SLOT_IDENTIFIER,
				HandCurioVisualCategory.PRIMARY_GAUNTLET,
				CHAMPIONS_GAUNTLET_PRIORITY,
				ChampionsGauntletModel.WIDE_LAYER_LOCATION,
				ChampionsGauntletModel.SLIM_LAYER_LOCATION,
				ChampionsGauntletModel::new,
				(player, arm, stack) -> CHAMPIONS_GAUNTLET_TEXTURE
		));
		register(new HandCurioVisualDefinition(
				TimothatysTrinketsModItems.DUELISTS_GAUNTLET.get(),
				CuriosHandsSlotHelper.HANDS_SLOT_IDENTIFIER,
				HandCurioVisualCategory.PRIMARY_GAUNTLET,
				DUELISTS_GAUNTLET_PRIORITY,
				DuelistsGauntletModel.WIDE_LAYER_LOCATION,
				DuelistsGauntletModel.SLIM_LAYER_LOCATION,
				DuelistsGauntletModel::new,
				(player, arm, stack) -> DUELISTS_GAUNTLET_TEXTURE
		));
		register(new HandCurioVisualDefinition(
				TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get(),
				CuriosBraceletSlotHelper.BRACELET_SLOT_IDENTIFIER,
				HandCurioVisualCategory.ARM_ACCESSORY,
				ARM_ACCESSORY_PRIORITY,
				UndeadKnightsArmletModel.WIDE_LAYER_LOCATION,
				UndeadKnightsArmletModel.SLIM_LAYER_LOCATION,
				UndeadKnightsArmletModel::new,
				(player, arm, stack) -> ArmletGauntletSynergyState.isPhysicalSynergyOnArm(player, arm)
						? UNDEAD_KNIGHTS_ARMLET_RIVETS_TEXTURE
						: UNDEAD_KNIGHTS_ARMLET_TEXTURE
		));
	}

	public static synchronized void register(HandCurioVisualDefinition definition) {
		HandCurioVisualDefinition previous = DEFINITIONS.putIfAbsent(definition.item(), definition);
		if (previous != null)
			throw new IllegalArgumentException("Hand-curio visual already registered for " + definition.item());
	}

	public static Optional<HandCurioVisualDefinition> find(Item item) {
		return Optional.ofNullable(DEFINITIONS.get(item));
	}

	public static Map<Item, HandCurioVisualDefinition> definitions() {
		return Collections.unmodifiableMap(DEFINITIONS);
	}

	private static ResourceLocation texture(String path) {
		return ResourceLocation.fromNamespaceAndPath(
				TimothatysTrinketsMod.MODID,
				"textures/curio/" + path + ".png"
		);
	}
}
