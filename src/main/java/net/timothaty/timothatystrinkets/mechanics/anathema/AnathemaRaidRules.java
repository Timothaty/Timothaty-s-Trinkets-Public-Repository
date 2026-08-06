package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;

public final class AnathemaRaidRules {
	private AnathemaRaidRules() {
	}

	public static boolean hasActiveRaid(ServerLevel level, BlockPos pos) {
		if (level == null || pos == null)
			return false;
		Raid raid = level.getRaidAt(pos);
		return raid != null && raid.isActive() && !raid.isStopped() && !raid.isOver();
	}
}
