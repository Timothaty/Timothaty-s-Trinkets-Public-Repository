package net.timothaty.timothatystrinkets.api.rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;

public final class RosariumTypes {
	public static final ResourceLocation HOLY = id("holy");
	public static final ResourceLocation CORRUPTED = id("corrupted");

	private RosariumTypes() {
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path);
	}
}
