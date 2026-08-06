package net.timothaty.timothatystrinkets;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class TimothatysTrinketsItemNameColors {
	private static final int BLASPHEMY_NAME_COLOR = 0x4E4B54;
	private static final int GNOSIS_NAME_COLOR = 0xC77DFF;
	private static final int SAINT_NAME_COLOR = 0xFFFFFF;

	private TimothatysTrinketsItemNameColors() {}

	@SubscribeEvent
	public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
		set(event, TimothatysTrinketsModItems.REFRESHING_CHALICE.get(), 0xE01D16);
		set(event, TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get(), 0x00FFA7);
		set(event, TimothatysTrinketsModItems.FANGS.get(), 0x92000A);
		set(event, TimothatysTrinketsModItems.HOLY_ROSARIUM.get(), 0xFBAE02);
		set(event, TimothatysTrinketsModItems.BELT_OF_OUTCAST.get(), 0x6C7059);
		set(event, TimothatysTrinketsModItems.PAGANS_CHARM.get(), 0x6C3B2A);
		set(event, TimothatysTrinketsModItems.CLEANSING_DUST.get(), 0xFF00FF);
		set(event, TimothatysTrinketsModItems.FARMERS_RING.get(), 0x009900);
		set(event, TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get(), 0x00FFA7);
		set(event, TimothatysTrinketsModItems.PILLAGERS_COIN.get(), 0xB3DBC7);
		set(event, TimothatysTrinketsModItems.DRUMS_OF_HASTE.get(), 0xFF3B1F);
		set(event, TimothatysTrinketsModItems.INDULGENCY.get(), 0xD8C28A);
		set(event, TimothatysTrinketsModItems.FLAMING_EMBER.get(), 0xE12D1C);
		set(event, TimothatysTrinketsModItems.RITUAL_DAGGER.get(), 0x11D111);
		set(event, TimothatysTrinketsModItems.NECRONOMICON.get(), 0xA52019);
		set(event, TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get(), 0xFFD36A);
		set(event, TimothatysTrinketsModItems.VOID_SPHERE.get(), 0x8A43C2);
		set(event, TimothatysTrinketsModItems.ECHO_SPHERE.get(), 0x55DFFF);
		set(event, TimothatysTrinketsModItems.FIRE_SPHERE.get(), 0xFF5100);
		set(event, TimothatysTrinketsModItems.VENOM_SPHERE.get(), 0xA9FF38);
		set(event, TimothatysTrinketsModItems.STRIKER_OF_THE_MORNING_STAR.get(), 0xAFB5B5);
		set(event, TimothatysTrinketsModItems.MORGENSHTERN.get(), 0xAFB5B5);
		set(event, TimothatysTrinketsModItems.PACT_OF_ALLIANCE.get(), 0x20519D);
		set(event, TimothatysTrinketsModItems.BEAD_OF_HUMILITY.get(), 0x1AC2F6);
		set(event, TimothatysTrinketsModItems.BEAD_OF_PRIDE.get(), 0x8A2638);
		set(event, TimothatysTrinketsModItems.BEAD_OF_REPENTANCE.get(), 0xF68F1A);
		set(event, TimothatysTrinketsModItems.BEAD_OF_SIN.get(), 0x8A681F);
		set(event, TimothatysTrinketsModItems.BEAD_OF_RESURRECTION.get(), 0xAB0002);
		set(event, TimothatysTrinketsModItems.BEAD_OF_THE_SACRAMENT.get(), 0x03AB00);
		set(event, TimothatysTrinketsModItems.BEAD_OF_THE_SAINT.get(), SAINT_NAME_COLOR);
		set(event, TimothatysTrinketsModItems.BEAD_OF_BLASPHEMY.get(), BLASPHEMY_NAME_COLOR);
		set(event, TimothatysTrinketsModItems.BEAD_OF_GNOSIS.get(), GNOSIS_NAME_COLOR);
		set(event, TimothatysTrinketsModItems.BEAD_OF_WRATH.get(), 0x930800);
		set(event, TimothatysTrinketsModItems.CORRUPTED_ROSARY.get(),  0xA22435);
		set(event, TimothatysTrinketsModItems.HOLY_INK.get(), 0xFFF79D);

	}

        private static void set(ModifyDefaultComponentsEvent event, Item item, int rgb) {
		event.modify(item, builder ->
			builder.set(DataComponents.ITEM_NAME, coloredName(item, rgb))
		);
	}

	private static Component coloredName(Item item, int rgb) {
		return Component.translatable(item.getDescriptionId())
			.withStyle(style -> style.withColor(rgb).withItalic(false));
	}
}
