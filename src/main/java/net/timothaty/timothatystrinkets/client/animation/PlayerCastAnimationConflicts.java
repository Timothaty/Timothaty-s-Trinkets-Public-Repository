package net.timothaty.timothatystrinkets.client.animation;

import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.client.gorge.GorgeAnimationState;
import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionClient;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PlayerCastAnimationConflicts {
	private PlayerCastAnimationConflicts() {
	}

	public static boolean hasVisualConflict(AbstractClientPlayer player) {
		if (StunnedClientAnimationState.isStunned(player)
				|| GorgeAnimationState.isActive(player)
				|| HubrisActivationClientState.isCasting(player)
				|| WrathOfTheWickedClientState.isActive(player)) {
			return true;
		}

		Minecraft minecraft = Minecraft.getInstance();
		return player == minecraft.player
				&& (DuelistGuardClient.isVisuallyGuarding()
				|| SoulOrbAbsorptionClient.isVisuallyChanneling());
	}
}
