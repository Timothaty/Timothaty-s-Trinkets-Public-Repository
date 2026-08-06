package net.timothaty.timothatystrinkets.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;

@OnlyIn(Dist.CLIENT)
public final class EncyclopaediaReliquiarumClient {
	private EncyclopaediaReliquiarumClient() {
	}

	public static void open() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.setScreen(new EncyclopaediaReliquiarumScreen());
		}
	}
}
