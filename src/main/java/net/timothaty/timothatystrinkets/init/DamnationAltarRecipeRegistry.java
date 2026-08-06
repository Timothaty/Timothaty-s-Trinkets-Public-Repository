package net.timothaty.timothatystrinkets.init;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarTransmutationRecipe;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarTransmutationRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DamnationAltarRecipeRegistry {
	public static final String ID = "damnation_altar_transmutation";
	public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TimothatysTrinketsMod.MODID);
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TimothatysTrinketsMod.MODID);

	public static final DeferredHolder<RecipeType<?>, RecipeType<DamnationAltarTransmutationRecipe>> TYPE = TYPES.register(ID, () -> new RecipeType<>() {
		@Override
		public String toString() {
			return TimothatysTrinketsMod.MODID + ":" + ID;
		}
	});

	public static final DeferredHolder<RecipeSerializer<?>, DamnationAltarTransmutationRecipeSerializer> SERIALIZER = SERIALIZERS.register(
			ID,
			DamnationAltarTransmutationRecipeSerializer::new
	);

	private DamnationAltarRecipeRegistry() {
	}
}
