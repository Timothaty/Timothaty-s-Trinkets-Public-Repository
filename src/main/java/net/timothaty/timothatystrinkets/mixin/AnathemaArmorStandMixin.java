package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaCrime;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaCrimes;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillageClaims;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillageRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class AnathemaArmorStandMixin {
	@Unique
	private Player timothatys_trinkets$pendingThief;
	@Unique
	private net.minecraft.core.BlockPos timothatys_trinkets$pendingTheftPos;
	@Unique
	private AnathemaCrimes.CrimeObservation timothatys_trinkets$pendingObservation;

	@Inject(method = "swapItem", at = @At("HEAD"))
	private void timothatys_trinkets$prepareArmorTheft(Player player, EquipmentSlot slot, ItemStack heldStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
		this.timothatys_trinkets$pendingThief = null;
		this.timothatys_trinkets$pendingTheftPos = null;
		this.timothatys_trinkets$pendingObservation = null;

		ArmorStand stand = (ArmorStand) (Object) this;
		if (!(stand.level() instanceof ServerLevel level) || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
			return;
		if (stand.getItemBySlot(slot).isEmpty())
			return;

		boolean playerPlaced = stand.getPersistentData().getBoolean(AnathemaVillageClaims.PLAYER_PLACED_ARMOR_STAND_KEY);
		if (!AnathemaVillageRules.isArmorerOwnedArmorStand(level, stand.blockPosition(), playerPlaced))
			return;
		AnathemaCrimes.CrimeObservation observation = AnathemaCrimes.observeWitnessedCrime(level, player, stand.blockPosition());
		if (observation == null)
			return;

		this.timothatys_trinkets$pendingThief = player;
		this.timothatys_trinkets$pendingTheftPos = stand.blockPosition().immutable();
		this.timothatys_trinkets$pendingObservation = observation;
	}

	@Inject(method = "swapItem", at = @At("RETURN"))
	private void timothatys_trinkets$finishArmorTheft(Player player, EquipmentSlot slot, ItemStack heldStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
		Player pendingPlayer = this.timothatys_trinkets$pendingThief;
		var theftPos = this.timothatys_trinkets$pendingTheftPos;
		AnathemaCrimes.CrimeObservation observation = this.timothatys_trinkets$pendingObservation;
		this.timothatys_trinkets$pendingThief = null;
		this.timothatys_trinkets$pendingTheftPos = null;
		this.timothatys_trinkets$pendingObservation = null;

		if (!cir.getReturnValueZ() || pendingPlayer != player || theftPos == null || observation == null)
			return;
		if (player.level() instanceof ServerLevel level)
			AnathemaCrimes.resolveObservedCrime(level, player, theftPos, AnathemaCrime.THEFT, observation);
	}
}
