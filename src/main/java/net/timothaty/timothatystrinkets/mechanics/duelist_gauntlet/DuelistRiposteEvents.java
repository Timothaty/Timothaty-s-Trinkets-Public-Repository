package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DuelistRiposteEvents {
	private static final int RIPOSTE_BLOOD_BITS = 32;
	private static final double RIPOSTE_BLOOD_SPEED = 0.085D;
	private static final int RIPOSTE_HIT_TRACK_TICKS = 2;
	private static final Map<RiposteHitKey, Long> PENDING_RIPOSTE_HITS = new HashMap<>();

	private DuelistRiposteEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;
		if (event.getEntity().level().isClientSide())
			return;

		LivingEntity attacker = DuelistMeleeDamage.getLivingAttacker(event.getSource());
		if (attacker == null || !attacker.hasEffect(TimothatysTrinketsModMobEffects.RIPOSTE))
			return;
		if (!DuelistMeleeDamage.hasMainHandSword(attacker))
			return;

		trackRiposteHit(attacker, event.getEntity());
		event.setNewDamage(event.getNewDamage() * DuelistGuardData.RIPOSTE_DAMAGE_MULTIPLIER);
		attacker.removeEffect(TimothatysTrinketsModMobEffects.RIPOSTE);
		spawnBloodBits(event.getEntity());
		playCounterAttackSound(event.getEntity(), attacker);
	}

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		if (event == null)
			return;
		if (event.getEntity().level().isClientSide())
			return;

		LivingEntity attacker = DuelistMeleeDamage.getLivingAttacker(event.getSource());
		if (!(attacker instanceof Player player))
			return;
		if (!consumeTrackedRiposteHit(player, event.getEntity()))
			return;
		if (event.getNewDamage() <= 0.0F)
			return;

		if (event.getEntity().isDeadOrDying() || event.getEntity().getHealth() <= 0.0F) {
			DuelistGuardState.refillStamina(player);
		} else {
			DuelistGuardState.addRiposteStamina(player);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event == null)
			return;
		if (event.getEntity().level().isClientSide())
			return;

		LivingEntity attacker = DuelistMeleeDamage.getLivingAttacker(event.getSource());
		if (attacker instanceof Player player && consumeTrackedRiposteHit(player, event.getEntity())) {
			DuelistGuardState.refillStamina(player);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_RIPOSTE_HITS.clear();
	}

	private static void spawnBloodBits(LivingEntity target) {
		if (!(target.level() instanceof ServerLevel serverLevel))
			return;

		double width = Math.max(0.24D, target.getBbWidth() * 0.52D);
		double height = Math.max(0.32D, target.getBbHeight() * 0.36D);
		serverLevel.sendParticles(TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(), target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(), RIPOSTE_BLOOD_BITS, width, height, width, RIPOSTE_BLOOD_SPEED);
	}

	private static void playCounterAttackSound(LivingEntity target, LivingEntity attacker) {
		target.level().playSound(null, target.blockPosition(), TimothatysTrinketsModSounds.COUNTER_ATTACK.get(), SoundSource.PLAYERS, 0.8F, 0.92F + attacker.getRandom().nextFloat() * 0.16F);
	}

	private static void trackRiposteHit(LivingEntity attacker, LivingEntity target) {
		if (!(attacker instanceof Player))
			return;
		long now = attacker.level().getGameTime();
		clearExpiredRiposteHits(now);
		PENDING_RIPOSTE_HITS.put(new RiposteHitKey(attacker.getId(), target.getId()), now);
	}

	private static boolean consumeTrackedRiposteHit(Player attacker, LivingEntity target) {
		long now = attacker.level().getGameTime();
		clearExpiredRiposteHits(now);
		Long hitTick = PENDING_RIPOSTE_HITS.remove(new RiposteHitKey(attacker.getId(), target.getId()));
		return hitTick != null && now - hitTick <= RIPOSTE_HIT_TRACK_TICKS;
	}

	private static void clearExpiredRiposteHits(long now) {
		Iterator<Map.Entry<RiposteHitKey, Long>> iterator = PENDING_RIPOSTE_HITS.entrySet().iterator();
		while (iterator.hasNext()) {
			if (now - iterator.next().getValue() > RIPOSTE_HIT_TRACK_TICKS) {
				iterator.remove();
			}
		}
	}

	private record RiposteHitKey(int attackerId, int targetId) {
	}
}
