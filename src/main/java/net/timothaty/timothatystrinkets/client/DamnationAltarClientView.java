package net.timothaty.timothatystrinkets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DamnationAltarClientView {
	private DamnationAltarClientView() {
	}

	public static double distanceToSqr(BlockPos pos) {
		Minecraft minecraft = Minecraft.getInstance();
		Entity viewer = minecraft.getCameraEntity();
		if (viewer == null)
			viewer = minecraft.player;
		if (viewer == null)
			return Double.POSITIVE_INFINITY;
		return viewer.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}
}
