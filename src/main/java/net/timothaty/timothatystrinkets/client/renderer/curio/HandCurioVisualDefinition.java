package net.timothaty.timothatystrinkets.client.renderer.curio;

import net.timothaty.timothatystrinkets.client.model.curio.HandCurioArmModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Function;

public record HandCurioVisualDefinition(
		Item item,
		String slotIdentifier,
		HandCurioVisualCategory category,
		int priority,
		ModelLayerLocation wideModelLayer,
		ModelLayerLocation slimModelLayer,
		Function<ModelPart, HandCurioArmModel> modelFactory,
		TextureResolver textureResolver
) {
	public HandCurioVisualDefinition {
		Objects.requireNonNull(item, "item");
		Objects.requireNonNull(slotIdentifier, "slotIdentifier");
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(wideModelLayer, "wideModelLayer");
		Objects.requireNonNull(slimModelLayer, "slimModelLayer");
		Objects.requireNonNull(modelFactory, "modelFactory");
		Objects.requireNonNull(textureResolver, "textureResolver");
	}

	@FunctionalInterface
	public interface TextureResolver {
		ResourceLocation resolve(AbstractClientPlayer player, HumanoidArm arm, ItemStack renderedStack);
	}
}
