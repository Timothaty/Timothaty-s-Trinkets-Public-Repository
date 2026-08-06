package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HolyRosariumStateEvents {
	private static final int SAFETY_RESYNC_INTERVAL_TICKS = 40;

	private HolyRosariumStateEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide())
			return;

		boolean refreshedDirtyState = HolyRosariumState.refreshIfDirty(player);
		if (!refreshedDirtyState && Math.floorMod(player.tickCount + player.getId(), SAFETY_RESYNC_INTERVAL_TICKS) == 0)
			HolyRosariumState.refreshNow(player);
	}

	@SubscribeEvent
	public static void onCurioChanged(CurioChangeEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!isRosarium(event.getFrom()) && !isRosarium(event.getTo()))
			return;

		HolyRosariumState.markDirty(player);
		HolyRosariumState.refreshNow(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAnathemaAdded(MobEffectEvent.Added event) {
		if (!(event.getEntity() instanceof Player player) || !isAnathema(event.getEffectInstance()))
			return;
		HolyRosariumState.setBonusesSuppressed(player, true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAnathemaRemoved(MobEffectEvent.Remove event) {
		if (!(event.getEntity() instanceof Player player)
				|| event.getEffect().value() != TimothatysTrinketsModMobEffects.ANATHEMA.get())
			return;
		if (!player.level().isClientSide() && AnathemaHelper.isRemovalAllowed(player)) {
			TimothatysTrinketsMod.queueServerWork(1, () -> HolyRosariumState.setBonusesSuppressed(
					player, player.hasEffect(TimothatysTrinketsModMobEffects.ANATHEMA)));
			return;
		}
		HolyRosariumState.setBonusesSuppressed(player, false);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAnathemaExpired(MobEffectEvent.Expired event) {
		if (!(event.getEntity() instanceof Player player) || !isAnathema(event.getEffectInstance()))
			return;
		HolyRosariumState.setBonusesSuppressed(player, false);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		initialize(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		HolyRosariumState.forget(event.getOriginal());
		initialize(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		initialize(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		HolyRosariumState.forget(event.getEntity());
	}

	public static void onRosariumDataChanged(Player player) {
		if (player == null)
			return;
		HolyRosariumState.markDirty(player);
		HolyRosariumState.refreshNow(player);
	}

	private static void initialize(Player player) {
		if (player == null)
			return;
		HolyRosariumState.forget(player);
		HolyRosariumState.refreshNow(player);
	}

	private static boolean isRosarium(ItemStack stack) {
		return stack != null && stack.is(TimothatysTrinketsModItems.HOLY_ROSARIUM.get());
	}

	private static boolean isAnathema(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.ANATHEMA.get();
	}
}
