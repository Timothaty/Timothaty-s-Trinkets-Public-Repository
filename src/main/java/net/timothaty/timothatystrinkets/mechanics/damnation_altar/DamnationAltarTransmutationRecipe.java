package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.timothaty.timothatystrinkets.init.DamnationAltarRecipeRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class DamnationAltarTransmutationRecipe implements Recipe<DamnationAltarRecipeInput> {
	private static final MapCodec<DamnationAltarTransmutationRecipe> RAW_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.LIST_CODEC_NONEMPTY.fieldOf("outer_ingredients").forGetter(DamnationAltarTransmutationRecipe::outerIngredients),
			Ingredient.CODEC_NONEMPTY.fieldOf("center_ingredient").forGetter(DamnationAltarTransmutationRecipe::centerIngredient),
			Result.CODEC.fieldOf("result").forGetter(DamnationAltarTransmutationRecipe::result),
			Codec.INT.fieldOf("duration").forGetter(DamnationAltarTransmutationRecipe::duration)
	).apply(instance, DamnationAltarTransmutationRecipe::new));

	public static final MapCodec<DamnationAltarTransmutationRecipe> CODEC = RAW_CODEC.validate(DamnationAltarTransmutationRecipe::validate);

	private final List<Ingredient> outerIngredients;
	private final Ingredient centerIngredient;
	private final Result result;
	private final int duration;

	public DamnationAltarTransmutationRecipe(List<Ingredient> outerIngredients, Ingredient centerIngredient, Result result, int duration) {
		this.outerIngredients = List.copyOf(outerIngredients);
		this.centerIngredient = centerIngredient;
		this.result = result;
		this.duration = duration;
	}

	private DataResult<DamnationAltarTransmutationRecipe> validate() {
		if (outerIngredients.size() != DamnationAltarSlot.OUTER_SLOTS.size()) {
			return DataResult.error(() -> "Damnation Altar transmutation requires exactly four outer ingredients");
		}
		if (duration <= 0) {
			return DataResult.error(() -> "Damnation Altar transmutation duration must be positive");
		}
		return result.validate().map(valid -> this);
	}

	@Override
	public boolean matches(DamnationAltarRecipeInput input, Level level) {
		if (input.size() != DamnationAltarSlot.values().length) return false;

		ItemStack center = input.getItem(DamnationAltarSlot.CENTER);
		if (center.getCount() != 1 || !centerIngredient.test(center)) return false;

		ItemStack[] outerStacks = new ItemStack[DamnationAltarSlot.OUTER_SLOTS.size()];
		for (int i = 0; i < outerStacks.length; i++) {
			outerStacks[i] = input.getItem(DamnationAltarSlot.OUTER_SLOTS.get(i));
			if (outerStacks[i].isEmpty()) return false;
		}
		return matchesOuterIngredient(0, outerStacks, new boolean[outerStacks.length]);
	}

	private boolean matchesOuterIngredient(int ingredientIndex, ItemStack[] stacks, boolean[] used) {
		if (ingredientIndex >= outerIngredients.size()) return true;
		Ingredient ingredient = outerIngredients.get(ingredientIndex);
		for (int stackIndex = 0; stackIndex < stacks.length; stackIndex++) {
			if (used[stackIndex] || !ingredient.test(stacks[stackIndex])) continue;
			used[stackIndex] = true;
			if (matchesOuterIngredient(ingredientIndex + 1, stacks, used)) return true;
			used[stackIndex] = false;
		}
		return false;
	}

	@Override
	public ItemStack assemble(DamnationAltarRecipeInput input, HolderLookup.Provider registries) {
		return result.previewStack();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= DamnationAltarSlot.values().length;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result.previewStack();
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		ingredients.addAll(outerIngredients);
		ingredients.add(centerIngredient);
		return ingredients;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return DamnationAltarRecipeRegistry.SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return DamnationAltarRecipeRegistry.TYPE.get();
	}

	public ItemStack rollResult(RandomSource random) {
		return result.roll(random);
	}

	public List<Ingredient> outerIngredients() {
		return outerIngredients;
	}

	public Ingredient centerIngredient() {
		return centerIngredient;
	}

	public Result result() {
		return result;
	}

	public int duration() {
		return duration;
	}

	public record Result(Item item, Optional<Integer> count, Optional<Integer> minCount, Optional<Integer> maxCount) {
		public static final Codec<Result> CODEC = RecordCodecBuilder.<Result>create(instance -> instance.group(
				BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(result -> result.item),
				Codec.INT.optionalFieldOf("count").forGetter(result -> result.count),
				Codec.INT.optionalFieldOf("min_count").forGetter(result -> result.minCount),
				Codec.INT.optionalFieldOf("max_count").forGetter(result -> result.maxCount)
		).apply(instance, Result::new)).validate(Result::validate);

		private DataResult<Result> validate() {
			if (new ItemStack(item).isEmpty()) return DataResult.error(() -> "Result item cannot be air");
			boolean fixed = count.isPresent();
			boolean ranged = minCount.isPresent() || maxCount.isPresent();
			if (fixed == ranged) {
				return DataResult.error(() -> "Result must use either count or both min_count and max_count");
			}
			if (ranged && (minCount.isEmpty() || maxCount.isEmpty())) {
				return DataResult.error(() -> "Ranged result requires both min_count and max_count");
			}

			int min = fixed ? count.orElse(0) : minCount.orElse(0);
			int max = fixed ? min : maxCount.orElse(0);
			if (min <= 0 || max <= 0) return DataResult.error(() -> "Result counts must be positive");
			if (min > max) return DataResult.error(() -> "Result min_count cannot exceed max_count");
			int maximumStackSize = new ItemStack(item).getMaxStackSize();
			if (max > maximumStackSize) {
				return DataResult.error(() -> "Result count " + max + " exceeds maximum stack size " + maximumStackSize);
			}
			return DataResult.success(this);
		}

		public ItemStack roll(RandomSource random) {
			int min = count.orElseGet(() -> minCount.orElse(1));
			int max = count.orElseGet(() -> maxCount.orElse(min));
			int rolled = min == max ? min : min + random.nextInt(max - min + 1);
			return new ItemStack(item, rolled);
		}

		public ItemStack previewStack() {
			return new ItemStack(item, count.orElseGet(() -> minCount.orElse(1)));
		}
	}
}
