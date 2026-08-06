package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@EventBusSubscriber
public class TimothatysTrinketsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TimothatysTrinketsMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TIMOTHATYS_TRINKETS_TAB = REGISTRY.register("timothatys_trinkets_tab", () -> CreativeModeTab.builder()
			.title(Component.translatable("item_group.timothatys_trinkets.timothatys_trinkets_tab")).icon(() -> new ItemStack(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get())).displayItems((parameters, tabData) -> {
				tabData.accept(TimothatysTrinketsModItems.ENCYCLOPAEDIA_RELIQUIARUM.get());
				tabData.accept(TimothatysTrinketsModItems.EMPTY_CHALICE.get());
				tabData.accept(TimothatysTrinketsModItems.REFRESHING_CHALICE.get());
				tabData.accept(TimothatysTrinketsModItems.BOTTLE_OF_BLOOD.get());
				tabData.accept(TimothatysTrinketsModItems.BOTTLE_OF_SOUL_ORB.get());
				tabData.accept(TimothatysTrinketsModItems.FANG.get());
				tabData.accept(TimothatysTrinketsModItems.FANGS.get());
				tabData.accept(TimothatysTrinketsModItems.UNHOLY_SHARD.get());
				tabData.accept(TimothatysTrinketsModItems.CURSED_EMERALD.get());
				tabData.accept(TimothatysTrinketsModItems.PILLAGERS_COIN.get());
				tabData.accept(TimothatysTrinketsModItems.FARMERS_RING.get());
				tabData.accept(TimothatysTrinketsModItems.PAGANS_CHARM.get());
				tabData.accept(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get());
				tabData.accept(TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get());
				tabData.accept(TimothatysTrinketsModItems.RUSTY_GAUNTLET.get());
				tabData.accept(TimothatysTrinketsModItems.RUSTY_ARMLET.get());
				tabData.accept(TimothatysTrinketsModItems.DUELISTS_GAUNTLET.get());
				tabData.accept(TimothatysTrinketsModItems.BELT_OF_OUTCAST.get());
				tabData.accept(TimothatysTrinketsModItems.DRUMS_OF_HASTE.get());
				tabData.accept(TimothatysTrinketsModItems.FLAMING_EMBER.get());
				tabData.accept(TimothatysTrinketsModItems.STRIKER_OF_THE_MORNING_STAR.get());
				tabData.accept(TimothatysTrinketsModItems.MORGENSHTERN.get());
				tabData.accept(TimothatysTrinketsModItems.CORRUPTED_INGOT.get());
				tabData.accept(TimothatysTrinketsModItems.RITUAL_DAGGER.get());
				tabData.accept(TimothatysTrinketsModItems.DAMNATION_ALTAR.get());
				tabData.accept(TimothatysTrinketsModItems.BLOCK_OF_BLIGHT.get());
				tabData.accept(TimothatysTrinketsModItems.DEATHBRINGER.get());
				tabData.accept(TimothatysTrinketsModItems.INDULGENCY.get());
				tabData.accept(TimothatysTrinketsModItems.HOLY_INK.get());
				tabData.accept(TimothatysTrinketsModItems.CLEANSING_DUST.get());
				tabData.accept(TimothatysTrinketsModItems.AROMATIC_OLIBANUM.get());
				tabData.accept(TimothatysTrinketsModItems.INCENSE.get());
				tabData.accept(TimothatysTrinketsModItems.FIRE_SPHERE.get());
				tabData.accept(TimothatysTrinketsModItems.VENOM_SPHERE.get());
				tabData.accept(TimothatysTrinketsModItems.VOID_SPHERE.get());
				tabData.accept(TimothatysTrinketsModItems.ECHO_SPHERE.get());
				tabData.accept(TimothatysTrinketsModItems.DORMANT_SPHERE.get());
				tabData.accept(TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get());
				tabData.accept(TimothatysTrinketsModItems.NECRONOMICON.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_HUMILITY.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_RESURRECTION.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_THE_SACRAMENT.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_REPENTANCE.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_BLASPHEMY.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_GNOSIS.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_PRIDE.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_SIN.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_WRATH.get());
				tabData.accept(TimothatysTrinketsModItems.BEAD_OF_THE_SAINT.get());
				tabData.accept(TimothatysTrinketsModItems.WOODEN_BEAD.get());
				tabData.accept(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get());
				tabData.accept(TimothatysTrinketsModItems.HOLY_ROSARIUM.get());
				tabData.accept(TimothatysTrinketsModItems.PACT_OF_ALLIANCE.get());
			}).build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(TimothatysTrinketsModItems.NECROMANCER_SPAWN_EGG.get());
			tabData.accept(TimothatysTrinketsModItems.UNDEAD_KNIGHT_SPAWN_EGG.get());
		}
	}
}
