package net.timothaty.timothatystrinkets.entity;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualSounds;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CleansingDustManifestationEntity extends AbstractItemManifestationEntity {
	public CleansingDustManifestationEntity(EntityType<? extends CleansingDustManifestationEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected ItemStack createRewardStack() {
		return new ItemStack(TimothatysTrinketsModItems.CLEANSING_DUST.get());
	}

	@Override
	protected int getPickupDelayTicks() {
		return 25;
	}

	@Override
	protected int getManifestationLifetimeTicks() {
		return 12 * 20;
	}

	@Override
	protected void onCollected(Player player) {
		CleansingRitualSounds.pickup(this.level(), this.blockPosition(), player);
	}
}
