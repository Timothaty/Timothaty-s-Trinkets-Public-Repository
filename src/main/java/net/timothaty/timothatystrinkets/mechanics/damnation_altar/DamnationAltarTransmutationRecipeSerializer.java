package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;

public final class DamnationAltarTransmutationRecipeSerializer implements RecipeSerializer<DamnationAltarTransmutationRecipe> {
	private static final StreamCodec<RegistryFriendlyByteBuf, DamnationAltarTransmutationRecipe.Result> RESULT_STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(DamnationAltarTransmutationRecipe.Result.CODEC);

	private static final StreamCodec<RegistryFriendlyByteBuf, DamnationAltarTransmutationRecipe> STREAM_CODEC = StreamCodec.of(
			DamnationAltarTransmutationRecipeSerializer::encode,
			DamnationAltarTransmutationRecipeSerializer::decode
	);

	@Override
	public MapCodec<DamnationAltarTransmutationRecipe> codec() {
		return DamnationAltarTransmutationRecipe.CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, DamnationAltarTransmutationRecipe> streamCodec() {
		return STREAM_CODEC;
	}

	private static void encode(RegistryFriendlyByteBuf buffer, DamnationAltarTransmutationRecipe recipe) {
		buffer.writeVarInt(recipe.outerIngredients().size());
		for (Ingredient ingredient : recipe.outerIngredients()) {
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
		}
		Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.centerIngredient());
		RESULT_STREAM_CODEC.encode(buffer, recipe.result());
		buffer.writeVarInt(recipe.duration());
	}

	private static DamnationAltarTransmutationRecipe decode(RegistryFriendlyByteBuf buffer) {
		int size = buffer.readVarInt();
		List<Ingredient> outerIngredients = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			outerIngredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
		}
		Ingredient centerIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
		DamnationAltarTransmutationRecipe.Result result = RESULT_STREAM_CODEC.decode(buffer);
		int duration = buffer.readVarInt();
		return new DamnationAltarTransmutationRecipe(outerIngredients, centerIngredient, result, duration);
	}
}
