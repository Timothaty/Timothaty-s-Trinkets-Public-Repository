package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearData;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.UUID;

public final class StrikerDebugCommands {
	private static final double TARGET_DISTANCE = 12.0D;
	private static final double TARGET_DISTANCE_SQR = TARGET_DISTANCE * TARGET_DISTANCE;

	private StrikerDebugCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
		return Commands.literal("striker")
			.then(Commands.literal("assign").executes(context -> assign(context.getSource())))
			.then(Commands.literal("status").executes(context -> status(context.getSource())))
			.then(Commands.literal("reset").executes(context -> reset(context.getSource())));
	}

	private static int assign(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Villager villager = findLookedAtVillager(player);
		if (villager == null) {
			source.sendFailure(Component.literal("Look at a villager within 12 blocks."));
			return 0;
		}
		ServerLevel level = player.serverLevel();
		if (!StrikerCommissionData.isEligibleForAssignment(level, villager)) {
			source.sendFailure(Component.literal(
					"A living adult Journeyman-or-higher weaponsmith with no active commission and a loaded grindstone job site is required."
			));
			return 0;
		}

		GlobalPos jobSite = StrikerCommissionData.getJobSite(villager).orElse(null);
		if (jobSite == null || !StrikerCommissionData.assignDebugCommission(villager, player.getUUID())) {
			source.sendFailure(Component.literal("Failed to assign a debug Striker commission."));
			return 0;
		}

		source.sendSuccess(() -> Component.literal(
				"Striker commission assigned: weaponsmith=" + villager.getUUID()
						+ ", recipient=" + player.getUUID()
						+ ", level=" + villager.getVillagerData().getLevel()
						+ ", job_site=" + formatGlobalPos(jobSite)
						+ ", stage=" + StrikerCommissionData.getStage(villager)
		), true);
		return 1;
	}

	private static int status(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer executor = source.getPlayerOrException();
		Villager villager = findLookedAtVillager(executor);
		if (villager == null) {
			source.sendFailure(Component.literal("Look at a villager within 12 blocks."));
			return 0;
		}

		ServerLevel level = executor.serverLevel();
		GlobalPos jobSite = StrikerCommissionData.getJobSite(villager).orElse(null);
		boolean sameDimension = jobSite != null && jobSite.dimension().equals(level.dimension());
		boolean grindstonePresent = sameDimension
				&& level.hasChunkAt(jobSite.pos())
				&& level.getBlockState(jobSite.pos()).is(Blocks.GRINDSTONE);
		double grindstoneDistance = sameDimension
				? Math.sqrt(villager.distanceToSqr(Vec3.atCenterOf(jobSite.pos())))
				: Double.NaN;
		boolean clearView = grindstonePresent
				&& StrikerForgingDeliveryGoal.hasClearWorkstationView(level, villager, jobSite.pos());

		UUID recipientId = StrikerCommissionData.getRecipientId(villager).orElse(null);
		ServerPlayer recipient = recipientId == null ? null : level.getServer().getPlayerList().getPlayer(recipientId);
		boolean fearsRecipient = recipientId != null && PillagersCoinVillagerFearData.fears(villager, recipientId);
		String raid = StrikerCommissionData.getRaidIdentity(villager)
				.map(identity -> identity.dimension() + "#" + identity.id() + "@" + identity.center().toShortString())
				.orElse("none");
		ItemStack hand = villager.getMainHandItem();
		String handItem = hand.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(hand.getItem()).toString();
		String profession = BuiltInRegistries.VILLAGER_PROFESSION
				.getKey(villager.getVillagerData().getProfession()).toString();

		sendLine(source, "profession=" + profession + ", level=" + villager.getVillagerData().getLevel());
		sendLine(source, "stage=" + StrikerCommissionData.getStage(villager) + ", recipient=" + value(recipientId));
		sendLine(source, "raid=" + raid);
		sendLine(source, "job_site=" + formatGlobalPos(jobSite)
				+ ", grindstone=" + grindstonePresent
				+ ", distance=" + formatDistance(grindstoneDistance)
				+ ", line_of_sight=" + clearView);
		sendLine(source, "trading_player=" + value(villager.getTradingPlayer() == null ? null : villager.getTradingPlayer().getUUID()));
		sendLine(source, "fear_recipient=" + fearsRecipient
				+ ", recipient_anathema=" + (recipient == null ? "offline" : AnathemaHelper.getLevel(recipient)));
		sendLine(source, "next_recipient_scan=" + StrikerCommissionData.getNextRecipientScan(villager)
				+ ", hand=" + handItem);
		return 1;
	}

	private static int reset(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Villager villager = findLookedAtVillager(player);
		if (villager == null) {
			source.sendFailure(Component.literal("Look at a villager within 12 blocks."));
			return 0;
		}

		StrikerCommissionData.clearCommission(player.serverLevel(), villager);
		source.sendSuccess(() -> Component.literal(
				"Striker commission reset for villager " + villager.getUUID() + "."
		), true);
		return 1;
	}

	private static Villager findLookedAtVillager(ServerPlayer player) {
		Vec3 start = player.getEyePosition();
		Vec3 direction = player.getViewVector(1.0F);
		Vec3 end = start.add(direction.scale(TARGET_DISTANCE));
		BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(
				start,
				end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player
		));
		double maxDistanceSqr = blockHit.getType() == HitResult.Type.MISS
				? TARGET_DISTANCE_SQR
				: start.distanceToSqr(blockHit.getLocation());
		AABB bounds = player.getBoundingBox().expandTowards(direction.scale(TARGET_DISTANCE)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				player,
				start,
				end,
				bounds,
				entity -> entity instanceof Villager && entity.isPickable() && !entity.isSpectator(),
				maxDistanceSqr
		);
		Entity target = hit == null ? null : hit.getEntity();
		return target instanceof Villager villager ? villager : null;
	}

	private static void sendLine(CommandSourceStack source, String text) {
		source.sendSuccess(() -> Component.literal(text), false);
	}

	private static String formatGlobalPos(GlobalPos pos) {
		return pos == null ? "none" : pos.dimension().location() + " " + pos.pos().toShortString();
	}

	private static String formatDistance(double distance) {
		return Double.isNaN(distance) ? "n/a" : String.format(Locale.ROOT, "%.2f", distance);
	}

	private static String value(Object value) {
		return value == null ? "none" : value.toString();
	}
}
