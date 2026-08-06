package net.timothaty.timothatystrinkets.block.entity;

import net.timothaty.timothatystrinkets.block.DamnationAltarBlock;
import net.timothaty.timothatystrinkets.client.DamnationAltarClientView;
import net.timothaty.timothatystrinkets.init.DamnationAltarRecipeRegistry;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarRecipeInput;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarSlot;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarSlotLayout;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarTransmutationRecipe;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordSummonManager;
import net.timothaty.timothatystrinkets.util.DamnationAltarOfferDisplayHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DamnationAltarBlockEntity extends BlockEntity {
	private static final String TAG_TRANSMUTING = "Transmuting";
	private static final String TAG_RECIPE_ID = "ActiveRecipe";
	private static final String TAG_START_TIME = "RitualStartTime";
	private static final String TAG_DURATION = "RitualDuration";
	private static final String TAG_PENDING_RESULT = "PendingResult";
	private static final String TAG_RESULT_PRESENTED = "ResultPresented";
	private static final String TAG_OFFER = "ExternalOffer";
	private static final String TAG_BLOOD_RITUAL_END_TIME = "BloodRitualEndTime";
	private static final String TAG_SACRIFICE_COUNT = "SacrificeCount";
	private static final String TAG_SACRIFICE_CYCLE = "SacrificeCycle";

	private static final int BEAM_PARTICLE_INTERVAL = 2;
	private static final int BEAM_POINTS = 7;
	private static final int ALTAR_STATE_INTERVAL_TICKS = 5;
	private static final int BLOOD_RITUAL_PARTICLE_INTERVAL_TICKS = 2;
	private static final int AMBIENT_DOTS_PER_ALTAR_TICK = 5;
	private static final int BLOOD_PARTICLE_SOURCES_PER_TICK = 8;
	private static final int MAX_SACRIFICES_PER_CYCLE = 35;
	private static final int ALTAR_RADIUS = 7;
	private static final int FLOOR_CACHE_REFRESH_INTERVAL_TICKS = 36;
	private static final int FLOOR_CACHE_PICK_RETRIES = 4;
	private static final double NEAR_PARTICLE_DISTANCE_SQR = 12.0D * 12.0D;
	private static final double MEDIUM_PARTICLE_DISTANCE_SQR = 20.0D * 20.0D;
	private static final double AMBIENT_PARTICLE_DISTANCE_SQR = 24.0D * 24.0D;
	private static final double MAX_PARTICLE_DISTANCE_SQR = 32.0D * 32.0D;
	private static final long NIGHT_START = 13000L;
	private static final double CANDLE_ONE_X = 2.5D;
	private static final double CANDLE_ONE_Y = 19.0D;
	private static final double CANDLE_ONE_Z = 10.0D;
	private static final double CANDLE_TWO_X = 5.5D;
	private static final double CANDLE_TWO_Y = 20.0D;
	private static final double CANDLE_TWO_Z = 13.0D;
	private static final double CANDLE_FLAME_X_OFFSET = 0.3D;
	private static final double CANDLE_FLAME_Y_OFFSET = 0.1D;
	private static final float TERRITORY_DOT_RED = 0x68 / 255.0F;
	private static final float TERRITORY_DOT_GREEN = 0xE6 / 255.0F;
	private static final float TERRITORY_DOT_BLUE = 0x23 / 255.0F;
	private static final DustParticleOptions ENERGY_DUST = new DustParticleOptions(new Vector3f(0x68 / 255.0F, 0xE6 / 255.0F, 0x23 / 255.0F), 0.9F);
	private static final DustParticleOptions RESULT_DUST_BRIGHT = new DustParticleOptions(new Vector3f(0x00 / 255.0F, 0x7B / 255.0F, 0x16 / 255.0F), 1.2F);
	private static final DustParticleOptions RESULT_DUST_DARK = new DustParticleOptions(new Vector3f(0x00 / 255.0F, 0x4A / 255.0F, 0x21 / 255.0F), 1.2F);
	private static final DustParticleOptions OFFER_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.15F, 0.85F), 1.0F);

	private final NonNullList<ItemStack> items = NonNullList.withSize(DamnationAltarSlot.values().length, ItemStack.EMPTY);
	private boolean transmuting;
	private ResourceLocation activeRecipeId;
	private long ritualStartTime;
	private int ritualDuration;
	private ItemStack pendingResult = ItemStack.EMPTY;
	private boolean resultPresented;
	private ItemStack externalOffer = ItemStack.EMPTY;
	private long bloodRitualEndTime;
	private int sacrificeCount;
	private long sacrificeCycle = Long.MIN_VALUE;
	private boolean suppressDestructionDrops;
	private int legacyMigrationAge;
	private boolean altarStateInitialized;

	private boolean recipeDirty = true;
	private RecipeHolder<DamnationAltarTransmutationRecipe> cachedRecipe;
	private final List<BlockPos> floorParticleSources = new ArrayList<>();
	private long floorParticleCacheNextRefresh = Long.MIN_VALUE;
	private boolean floorParticleCacheInitialized;

	public DamnationAltarBlockEntity(BlockPos pos, BlockState state) {
		super(TimothatysTrinketsModBlockEntities.DAMNATION_ALTAR.get(), pos, state);
	}

	public ItemStack getStack(DamnationAltarSlot slot) {
		return items.get(slot.index());
	}

	public boolean hasStoredItems() {
		return items.stream().anyMatch(stack -> !stack.isEmpty());
	}

	public boolean isTransmuting() {
		return transmuting;
	}

	public boolean isResultPresented() {
		return resultPresented;
	}

	public ItemStack getExternalOffer() {
		return externalOffer;
	}

	public boolean hasExternalOffer() {
		return !externalOffer.isEmpty();
	}

	public boolean trySetExternalOffer(ItemStack stack) {
		if (stack == null || stack.isEmpty() || hasExternalOffer() || transmuting || hasStoredItems()) return false;
		externalOffer = stack.copy();
		sync();
		return true;
	}

	public boolean adoptLegacyExternalOffer(ItemStack stack) {
		if (stack == null || stack.isEmpty() || hasExternalOffer() || suppressDestructionDrops) return false;
		externalOffer = stack.copy();
		sync();
		return true;
	}

	public ItemStack takeExternalOffer() {
		if (externalOffer.isEmpty()) return ItemStack.EMPTY;
		ItemStack taken = externalOffer.copy();
		externalOffer = ItemStack.EMPTY;
		sync();
		return taken;
	}

	public boolean isBusyForExternalRitual() {
		return transmuting || hasStoredItems();
	}

	public boolean canAcceptTransmutationInteraction() {
		return !transmuting && !hasExternalOffer();
	}

	public boolean hasSacrificeCapacity(ServerLevel level) {
		refreshSacrificeCycle(level);
		return sacrificeCount < MAX_SACRIFICES_PER_CYCLE;
	}

	public boolean tryConsumeSacrifice(ServerLevel level) {
		refreshSacrificeCycle(level);
		if (sacrificeCount >= MAX_SACRIFICES_PER_CYCLE) return false;
		sacrificeCount++;
		sync();
		if (sacrificeCount >= MAX_SACRIFICES_PER_CYCLE) updateLitState(level, true);
		return true;
	}

	public void disableSacrificesUntilNextNight(ServerLevel level) {
		refreshSacrificeCycle(level);
		sacrificeCount = MAX_SACRIFICES_PER_CYCLE;
		sync();
		updateLitState(level, true);
	}

	public void startBloodRitual(ServerLevel level, int durationTicks) {
		if (durationTicks <= 0) return;
		bloodRitualEndTime = level.getGameTime() + durationTicks;
		sync();
	}

	public boolean isBloodRitualActive(long gameTime) {
		return bloodRitualEndTime > gameTime;
	}

	public long getRitualStartTime() {
		return ritualStartTime;
	}

	public int getRitualDuration() {
		return ritualDuration;
	}

	public ResourceLocation getActiveRecipeId() {
		return activeRecipeId;
	}

	public boolean insertOne(DamnationAltarSlot slot, ItemStack source, boolean creative) {
		if (transmuting || hasExternalOffer() || source.isEmpty()) return false;
		ItemStack stored = getStack(slot);
		if (stored.isEmpty()) {
			items.set(slot.index(), source.copyWithCount(1));
		} else {
			if (!ItemStack.isSameItemSameComponents(stored, source) || stored.getCount() >= stored.getMaxStackSize()) return false;
			stored.grow(1);
		}
		if (!creative) source.shrink(1);
		if (slot == DamnationAltarSlot.CENTER) resultPresented = false;
		onInventoryChanged();
		return true;
	}

	public ItemStack extractOne(DamnationAltarSlot slot) {
		if (transmuting) return ItemStack.EMPTY;
		ItemStack stored = getStack(slot);
		if (stored.isEmpty()) return ItemStack.EMPTY;
		ItemStack extracted = stored.split(1);
		if (stored.isEmpty()) items.set(slot.index(), ItemStack.EMPTY);
		if (slot == DamnationAltarSlot.CENTER && stored.isEmpty()) resultPresented = false;
		onInventoryChanged();
		return extracted;
	}

	public Optional<RecipeHolder<DamnationAltarTransmutationRecipe>> getMatchingRecipe(ServerLevel level) {
		if (transmuting || hasExternalOffer()) return Optional.empty();
		if (recipeDirty) {
			cachedRecipe = level.getRecipeManager()
					.getRecipeFor(DamnationAltarRecipeRegistry.TYPE.get(), createRecipeInput(), level)
					.orElse(null);
			recipeDirty = false;
		}
		return Optional.ofNullable(cachedRecipe);
	}

	public boolean startTransmutation(ServerLevel level, RecipeHolder<DamnationAltarTransmutationRecipe> recipeHolder) {
		if (transmuting || hasExternalOffer() || recipeHolder == null || !recipeHolder.value().matches(createRecipeInput(), level)) return false;
		ItemStack rolledResult = recipeHolder.value().rollResult(level.getRandom());
		if (rolledResult.isEmpty()) return false;

		activeRecipeId = recipeHolder.id();
		ritualStartTime = level.getGameTime();
		ritualDuration = recipeHolder.value().duration();
		pendingResult = rolledResult.copy();
		resultPresented = false;
		transmuting = true;
		cachedRecipe = recipeHolder;
		recipeDirty = false;
		sync();

		level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.75F);
		return true;
	}

	public void cancelForDestruction() {
		if (suppressDestructionDrops) return;
		suppressDestructionDrops = transmuting;
		if (!transmuting) return;
		clearItemsWithoutDrops();
		externalOffer = ItemStack.EMPTY;
		clearActiveRitual();
		resultPresented = false;
		setChanged();
	}

	public boolean shouldSuppressDestructionDrops() {
		return suppressDestructionDrops;
	}

	public void dropStoredItems() {
		if (level == null || level.isClientSide || suppressDestructionDrops) return;
		NonNullList<ItemStack> drops = NonNullList.withSize(items.size(), ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++) drops.set(i, items.get(i).copy());
		ItemStack offerDrop = externalOffer.copy();

		clearItemsWithoutDrops();
		externalOffer = ItemStack.EMPTY;
		setChanged();
		Containers.dropContents(level, worldPosition, drops);
		if (!offerDrop.isEmpty()) {
			Containers.dropItemStack(level,
					worldPosition.getX() + 0.5D,
					worldPosition.getY() + 1.0D,
					worldPosition.getZ() + 0.5D,
					offerDrop);
		}
	}

	public float getElapsedRitualTicks(float partialTick) {
		if (!transmuting || level == null) return 0.0F;
		return Math.max(0.0F, level.getGameTime() - ritualStartTime + partialTick);
	}

	public double getCenterRenderY(float partialTick) {
		if (!transmuting) return resultPresented ? DamnationAltarSlotLayout.CENTER_RAISED_Y : DamnationAltarSlotLayout.CENTER_IDLE_Y;
		float elapsed = getElapsedRitualTicks(partialTick);
		float riseStart = phaseBoundary(0.4F);
		float riseEnd = phaseBoundary(0.8F);
		if (elapsed <= riseStart) return DamnationAltarSlotLayout.CENTER_IDLE_Y;
		if (elapsed >= riseEnd) return DamnationAltarSlotLayout.CENTER_RAISED_Y;
		float progress = (elapsed - riseStart) / Math.max(1.0F, riseEnd - riseStart);
		float smooth = progress * progress * (3.0F - 2.0F * progress);
		return Mth.lerp(smooth, DamnationAltarSlotLayout.CENTER_IDLE_Y, DamnationAltarSlotLayout.CENTER_RAISED_Y);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DamnationAltarBlockEntity altar) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		long gameTime = serverLevel.getGameTime();

		altar.tickLegacyOfferMigration(serverLevel);
		if (gameTime % ALTAR_STATE_INTERVAL_TICKS == 0L) {
			altar.refreshSacrificeCycle(serverLevel);
			if (altar.bloodRitualEndTime != 0L && altar.bloodRitualEndTime <= gameTime) {
				altar.bloodRitualEndTime = 0L;
				altar.sync();
			}
			altar.updateLitState(serverLevel, !altar.altarStateInitialized);
		}

		if (!altar.transmuting) return;
		long elapsed = Math.max(0L, serverLevel.getGameTime() - altar.ritualStartTime);
		if (elapsed >= altar.ritualDuration) {
			altar.completeTransmutation(serverLevel);
			return;
		}

		int riseStart = altar.phaseBoundary(0.4F);
		if (elapsed == riseStart) {
			serverLevel.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.85F, 1.2F);
		}
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, DamnationAltarBlockEntity altar) {
		if (!level.isClientSide) return;
		long gameTime = level.getGameTime();
		double viewerDistanceSqr = DamnationAltarClientView.distanceToSqr(pos);
		if (viewerDistanceSqr > MAX_PARTICLE_DISTANCE_SQR) return;
		int detailTier = viewerDistanceSqr <= NEAR_PARTICLE_DISTANCE_SQR
				? 0
				: viewerDistanceSqr <= MEDIUM_PARTICLE_DISTANCE_SQR ? 1 : 2;
		int beamPoints = detailTier == 0 ? BEAM_POINTS : detailTier == 1 ? 4 : 2;
		int bloodSources = detailTier == 0 ? BLOOD_PARTICLE_SOURCES_PER_TICK : detailTier == 1 ? 4 : 2;
		boolean structurallyUsable = !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above());

		if (altar.transmuting) {
			long elapsed = Math.max(0L, gameTime - altar.ritualStartTime);
			int riseStart = altar.phaseBoundary(0.4F);
			int quietStart = altar.phaseBoundary(0.8F);
			if (elapsed < quietStart && isPhasedParticleTick(gameTime, pos, BEAM_PARTICLE_INTERVAL)) {
				altar.spawnEnergyBeams(level, state, elapsed, riseStart, beamPoints);
			}
		}

		if (structurallyUsable && altar.hasExternalOffer()) {
			int offerInterval = detailTier < 2 ? 1 : 2;
			if (isPhasedParticleTick(gameTime, pos, offerInterval)) {
				altar.spawnOfferDust(level, detailTier == 0 ? 2 : 1);
			}
		}

		boolean bloodRitualActive = altar.isBloodRitualActive(gameTime);
		if (bloodRitualActive && structurallyUsable
				&& isPhasedParticleTick(gameTime, pos, BLOOD_RITUAL_PARTICLE_INTERVAL_TICKS)) {
			altar.spawnBloodTerritoryParticles(level, bloodSources);
		}

		if (state.getValue(DamnationAltarBlock.LIT)
				&& viewerDistanceSqr <= AMBIENT_PARTICLE_DISTANCE_SQR
				&& isPhasedParticleTick(gameTime, pos, ALTAR_STATE_INTERVAL_TICKS)
				) {
			altar.spawnCandleFlames(level, state);
			if (!bloodRitualActive) {
				int ambientDots = detailTier == 0 ? AMBIENT_DOTS_PER_ALTAR_TICK : detailTier == 1 ? 3 : 1;
				altar.spawnAmbientTerritoryDots(level, ambientDots);
			}
		}
	}

	private static boolean isPhasedParticleTick(long gameTime, BlockPos pos, int interval) {
		long phaseSeed = pos.asLong();
		phaseSeed ^= phaseSeed >>> 33;
		phaseSeed *= 0xff51afd7ed558ccdL;
		phaseSeed ^= phaseSeed >>> 33;
		long phase = Math.floorMod(phaseSeed, (long) interval);
		return Math.floorMod(gameTime + phase, (long) interval) == 0L;
	}

	private void completeTransmutation(ServerLevel level) {
		if (!transmuting) return;
		for (DamnationAltarSlot slot : DamnationAltarSlot.OUTER_SLOTS) {
			ItemStack stack = getStack(slot);
			stack.shrink(1);
			if (stack.isEmpty()) items.set(slot.index(), ItemStack.EMPTY);
		}
		ItemStack center = getStack(DamnationAltarSlot.CENTER);
		center.shrink(1);
		items.set(DamnationAltarSlot.CENTER.index(), pendingResult.copy());
		resultPresented = true;
		clearActiveRitual();
		recipeDirty = true;
		cachedRecipe = null;
		sync();

		spawnResultBurst(level);
		level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.1F, 0.9F);
	}

	private void spawnEnergyBeams(Level level, BlockState state, long elapsed, int growthEnd, int beamPoints) {
		Direction facing = state.getValue(DamnationAltarBlock.FACING);
		double centerY = getCenterRenderY(0.0F);
		double beamLength = growthEnd <= 0 ? 1.0D : Mth.clamp((double) elapsed / growthEnd, 0.0D, 1.0D);
		DamnationAltarSlotLayout.LocalPoint center = DamnationAltarSlotLayout.getWorldPosition(facing, DamnationAltarSlot.CENTER);

		for (DamnationAltarSlot slot : DamnationAltarSlot.OUTER_SLOTS) {
			DamnationAltarSlotLayout.LocalPoint outer = DamnationAltarSlotLayout.getWorldPosition(facing, slot);
			for (int point = 1; point <= beamPoints; point++) {
				double t = beamLength * point / beamPoints;
				double x = Mth.lerp(t, outer.x(), center.x());
				double y = Mth.lerp(t, DamnationAltarSlotLayout.OUTER_ITEM_Y + 0.04D, centerY);
				double z = Mth.lerp(t, outer.z(), center.z());
				level.addParticle(ENERGY_DUST, worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	private void spawnResultBurst(ServerLevel level) {
		for (int i = 0; i < 32; i++) {
			double angle = Math.PI * 2.0D * i / 32.0D + level.getRandom().nextDouble() * 0.16D;
			double horizontal = 0.08D + level.getRandom().nextDouble() * 0.09D;
			double vx = Math.cos(angle) * horizontal;
			double vy = 0.025D + level.getRandom().nextDouble() * 0.08D;
			double vz = Math.sin(angle) * horizontal;
			DustParticleOptions dust = (i & 1) == 0 ? RESULT_DUST_BRIGHT : RESULT_DUST_DARK;
			level.sendParticles(dust,
					worldPosition.getX() + 0.5D, worldPosition.getY() + DamnationAltarSlotLayout.CENTER_RAISED_Y, worldPosition.getZ() + 0.5D,
					0, vx, vy, vz, 1.0D);
		}
	}

	private void tickLegacyOfferMigration(ServerLevel level) {
		legacyMigrationAge++;
		if (legacyMigrationAge == 1 || legacyMigrationAge == 20 || legacyMigrationAge == 100) {
			DamnationAltarOfferDisplayHandler.migrateLegacyDisplaysAt(level, worldPosition);
		}
	}

	private void refreshSacrificeCycle(ServerLevel level) {
		long currentCycle = Math.floorDiv(level.getDayTime() - NIGHT_START, 24000L);
		if (sacrificeCycle == currentCycle) return;

		boolean capacityChanged = sacrificeCount != 0;
		sacrificeCycle = currentCycle;
		sacrificeCount = 0;
		if (capacityChanged) sync();
		else setChanged();
	}

	private void updateLitState(ServerLevel level, boolean allowFallbackBossLookup) {
		boolean structurallyUsable = level.getBlockState(worldPosition).getBlock() instanceof DamnationAltarBlock
				&& !level.getBlockState(worldPosition.above()).isCollisionShapeFullBlock(level, worldPosition.above());
		boolean bossActive = allowFallbackBossLookup
				? DebtlordSummonManager.isAltarActive(level, worldPosition)
				: DebtlordSummonManager.hasRegisteredAltarLock(level, worldPosition);
		boolean lit = structurallyUsable && !bossActive && sacrificeCount < MAX_SACRIFICES_PER_CYCLE;
		altarStateInitialized = true;

		BlockState state = level.getBlockState(worldPosition);
		if (state.getBlock() instanceof DamnationAltarBlock && state.getValue(DamnationAltarBlock.LIT) != lit) {
			level.setBlock(worldPosition, state.setValue(DamnationAltarBlock.LIT, lit), 3);
		}
	}

	private void spawnOfferDust(Level level, int particleCount) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < particleCount; i++) {
			level.addParticle(
					OFFER_DUST,
					worldPosition.getX() + 0.5D + random.nextGaussian() * 0.25D,
					worldPosition.getY() + 1.05D + random.nextGaussian() * 0.10D,
					worldPosition.getZ() + 0.5D + random.nextGaussian() * 0.25D,
					0.0D, 0.0D, 0.0D
			);
		}
	}

	private void spawnAmbientTerritoryDots(Level level, int particleCount) {
		for (int i = 0; i < particleCount; i++) {
			BlockPos source = pickFloorParticleSource(level);
			if (source == null) return;
			RandomSource random = level.getRandom();
			double x = source.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
			double y = source.getY() + 1.03D;
			double z = source.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
			level.addParticle(TimothatysTrinketsModParticleTypes.DOT.get(), x, y, z,
					TERRITORY_DOT_RED, TERRITORY_DOT_GREEN, TERRITORY_DOT_BLUE);
		}
	}

	private void spawnBloodTerritoryParticles(Level level, int sourceCount) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < sourceCount; i++) {
			BlockPos source = pickFloorParticleSource(level);
			double baseX = source == null ? worldPosition.getX() + 0.5D : source.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
			double baseY = source == null ? worldPosition.getY() + 1.05D : source.getY() + 1.05D;
			double baseZ = source == null ? worldPosition.getZ() + 0.5D : source.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
			for (int particle = 0; particle < 2; particle++) {
				level.addParticle(
						TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
						baseX + random.nextGaussian() * 0.06D,
						baseY + random.nextGaussian() * 0.08D,
						baseZ + random.nextGaussian() * 0.06D,
						random.nextGaussian() * 0.035D,
						random.nextGaussian() * 0.035D,
						random.nextGaussian() * 0.035D
				);
			}
		}
	}

	private BlockPos pickFloorParticleSource(Level level) {
		long gameTime = level.getGameTime();
		if (!floorParticleCacheInitialized || gameTime >= floorParticleCacheNextRefresh) {
			rebuildFloorParticleCache(level, gameTime);
		}
		if (floorParticleSources.isEmpty()) {
			return null;
		}

		RandomSource random = level.getRandom();
		int attempts = Math.min(FLOOR_CACHE_PICK_RETRIES, floorParticleSources.size());
		for (int attempt = 0; attempt < attempts && !floorParticleSources.isEmpty(); attempt++) {
			int selectedIndex = random.nextInt(floorParticleSources.size());
			BlockPos selected = floorParticleSources.get(selectedIndex);
			if (isValidFloorParticleSource(level, selected)) {
				return selected;
			}
			removeFloorParticleSource(selectedIndex);
		}
		floorParticleCacheNextRefresh = Math.min(floorParticleCacheNextRefresh, gameTime + 1L);
		return null;
	}

	private void rebuildFloorParticleCache(Level level, long gameTime) {
		floorParticleSources.clear();
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		int radiusSqr = ALTAR_RADIUS * ALTAR_RADIUS;
		for (int dx = -ALTAR_RADIUS; dx <= ALTAR_RADIUS; dx++) {
			for (int dz = -ALTAR_RADIUS; dz <= ALTAR_RADIUS; dz++) {
				if (dx * dx + dz * dz > radiusSqr)
					continue;
				BlockPos source = findFloorParticleSourceAt(
						level,
						mutablePos,
						worldPosition.getX() + dx,
						worldPosition.getZ() + dz
				);
				if (source != null)
					floorParticleSources.add(source);
			}
		}
		floorParticleCacheInitialized = true;
		floorParticleCacheNextRefresh = nextPhasedFloorCacheRefresh(gameTime);
	}

	private BlockPos findFloorParticleSourceAt(Level level, BlockPos.MutableBlockPos mutablePos, int x, int z) {
		for (int dy = 0; dy >= -3; dy--) {
			mutablePos.set(x, worldPosition.getY() + dy, z);
			if (!level.hasChunkAt(mutablePos))
				return null;
			BlockState state = level.getBlockState(mutablePos);
			if (!(state.getBlock() instanceof DamnationAltarBlock)
					&& !state.isAir()
					&& state.isCollisionShapeFullBlock(level, mutablePos)) return mutablePos.immutable();
		}
		return null;
	}

	private static boolean isValidFloorParticleSource(Level level, BlockPos source) {
		if (!level.hasChunkAt(source))
			return false;
		BlockState state = level.getBlockState(source);
		return !(state.getBlock() instanceof DamnationAltarBlock)
				&& !state.isAir()
				&& state.isCollisionShapeFullBlock(level, source);
	}

	private void removeFloorParticleSource(int index) {
		int lastIndex = floorParticleSources.size() - 1;
		if (index != lastIndex)
			floorParticleSources.set(index, floorParticleSources.get(lastIndex));
		floorParticleSources.remove(lastIndex);
	}

	private long nextPhasedFloorCacheRefresh(long gameTime) {
		long phase = Math.floorMod(mixPosition(worldPosition.asLong()), FLOOR_CACHE_REFRESH_INTERVAL_TICKS);
		long currentPhase = Math.floorMod(gameTime, FLOOR_CACHE_REFRESH_INTERVAL_TICKS);
		long delay = Math.floorMod(phase - currentPhase, FLOOR_CACHE_REFRESH_INTERVAL_TICKS);
		return gameTime + (delay == 0L ? FLOOR_CACHE_REFRESH_INTERVAL_TICKS : delay);
	}

	private static long mixPosition(long value) {
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		return value ^ value >>> 33;
	}

	private void spawnCandleFlames(Level level, BlockState state) {
		spawnCandleFlame(level, state, CANDLE_ONE_X + CANDLE_FLAME_X_OFFSET, CANDLE_ONE_Y, CANDLE_ONE_Z);
		spawnCandleFlame(level, state, CANDLE_TWO_X + CANDLE_FLAME_X_OFFSET, CANDLE_TWO_Y, CANDLE_TWO_Z);
	}

	private void spawnCandleFlame(Level level, BlockState state, double modelX, double modelY, double modelZ) {
		Direction facing = state.getValue(DamnationAltarBlock.FACING);
		double localX = modelX / 16.0D - 0.5D;
		double localZ = modelZ / 16.0D - 0.5D;
		double rotatedX;
		double rotatedZ;
		switch (facing) {
			case EAST -> {
				rotatedX = -localZ;
				rotatedZ = localX;
			}
			case SOUTH -> {
				rotatedX = -localX;
				rotatedZ = -localZ;
			}
			case WEST -> {
				rotatedX = localZ;
				rotatedZ = -localX;
			}
			default -> {
				rotatedX = localX;
				rotatedZ = localZ;
			}
		}
		RandomSource random = level.getRandom();
		level.addParticle(
				ParticleTypes.SMALL_FLAME,
				worldPosition.getX() + 0.5D + rotatedX + random.nextGaussian() * 0.01D,
				worldPosition.getY() + modelY / 16.0D + CANDLE_FLAME_Y_OFFSET + random.nextGaussian() * 0.01D,
				worldPosition.getZ() + 0.5D + rotatedZ + random.nextGaussian() * 0.01D,
				0.0D, 0.0D, 0.0D
		);
	}

	private int phaseBoundary(float fraction) {
		return Math.max(1, Mth.floor(ritualDuration * fraction));
	}

	private DamnationAltarRecipeInput createRecipeInput() {
		return new DamnationAltarRecipeInput(items);
	}

	private void onInventoryChanged() {
		recipeDirty = true;
		cachedRecipe = null;
		sync();
	}

	private void clearItemsWithoutDrops() {
		for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
		recipeDirty = true;
		cachedRecipe = null;
	}

	private void clearActiveRitual() {
		transmuting = false;
		activeRecipeId = null;
		ritualStartTime = 0L;
		ritualDuration = 0;
		pendingResult = ItemStack.EMPTY;
	}

	private void sync() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, 3);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
		tag.putBoolean(TAG_TRANSMUTING, transmuting);
		if (activeRecipeId != null) tag.putString(TAG_RECIPE_ID, activeRecipeId.toString());
		tag.putLong(TAG_START_TIME, ritualStartTime);
		tag.putInt(TAG_DURATION, ritualDuration);
		if (!pendingResult.isEmpty()) tag.put(TAG_PENDING_RESULT, pendingResult.save(registries));
		tag.putBoolean(TAG_RESULT_PRESENTED, resultPresented);
		if (!externalOffer.isEmpty()) tag.put(TAG_OFFER, externalOffer.save(registries));
		if (bloodRitualEndTime > 0L) tag.putLong(TAG_BLOOD_RITUAL_END_TIME, bloodRitualEndTime);
		tag.putInt(TAG_SACRIFICE_COUNT, sacrificeCount);
		if (sacrificeCycle != Long.MIN_VALUE) tag.putLong(TAG_SACRIFICE_CYCLE, sacrificeCycle);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		for (int i = 0; i < items.size(); i++) {
			items.set(i, ItemStack.EMPTY);
		}
		ContainerHelper.loadAllItems(tag, items, registries);
		transmuting = tag.getBoolean(TAG_TRANSMUTING);
		activeRecipeId = tag.contains(TAG_RECIPE_ID, Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString(TAG_RECIPE_ID)) : null;
		ritualStartTime = tag.getLong(TAG_START_TIME);
		ritualDuration = tag.getInt(TAG_DURATION);
		pendingResult = tag.contains(TAG_PENDING_RESULT, Tag.TAG_COMPOUND)
				? ItemStack.parseOptional(registries, tag.getCompound(TAG_PENDING_RESULT))
				: ItemStack.EMPTY;
		resultPresented = tag.getBoolean(TAG_RESULT_PRESENTED);
		externalOffer = tag.contains(TAG_OFFER, Tag.TAG_COMPOUND)
				? ItemStack.parseOptional(registries, tag.getCompound(TAG_OFFER))
				: ItemStack.EMPTY;
		bloodRitualEndTime = tag.getLong(TAG_BLOOD_RITUAL_END_TIME);
		sacrificeCount = Mth.clamp(tag.getInt(TAG_SACRIFICE_COUNT), 0, MAX_SACRIFICES_PER_CYCLE);
		sacrificeCycle = tag.contains(TAG_SACRIFICE_CYCLE, Tag.TAG_LONG)
				? tag.getLong(TAG_SACRIFICE_CYCLE)
				: Long.MIN_VALUE;
		suppressDestructionDrops = false;
		legacyMigrationAge = 0;
		altarStateInitialized = false;
		recipeDirty = true;
		cachedRecipe = null;

		if (transmuting && (activeRecipeId == null || ritualDuration <= 0 || pendingResult.isEmpty())) {
			clearActiveRitual();
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
