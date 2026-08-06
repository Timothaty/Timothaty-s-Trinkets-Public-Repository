package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.FlamingEmberData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FlamingEmberItem extends Item {
	private static final int BLAZE_POWDER_HEAT_MIN = 1;
	private static final int BLAZE_POWDER_HEAT_MAX = 4;

	public FlamingEmberItem() {
		super(new Item.Properties().stacksTo(1).fireResistant());
	}

	@Override
	public boolean overrideOtherStackedOnMe(
			ItemStack ember,
			ItemStack otherStack,
			Slot slot,
			ClickAction action,
			Player player,
			SlotAccess carriedAccess
	) {
		if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY)
			return false;
		if (!otherStack.is(Items.BLAZE_POWDER))
			return false;

		if (FlamingEmberData.getHeat(ember) >= FlamingEmberData.MAX_HEAT)
			return true;

		int heatGain = BLAZE_POWDER_HEAT_MIN + player.getRandom().nextInt(BLAZE_POWDER_HEAT_MAX - BLAZE_POWDER_HEAT_MIN + 1);
		FlamingEmberData.addHeat(ember, heatGain);
		otherStack.shrink(1);

		playBlazePowderSound(player);

		if (otherStack.isEmpty())
			carriedAccess.set(ItemStack.EMPTY);

		slot.setChanged();
		return true;
	}

	private static void playBlazePowderSound(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return;

		serverPlayer.playNotifySound(
				TimothatysTrinketsModSounds.EMBER_BLAZE_POWDER.get(),
				SoundSource.PLAYERS,
				0.75F,
				0.9F + player.getRandom().nextFloat() * 0.2F
		);
	}
}
