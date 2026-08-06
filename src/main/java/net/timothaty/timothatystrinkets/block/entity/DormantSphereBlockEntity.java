package net.timothaty.timothatystrinkets.block.entity;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

public final class DormantSphereBlockEntity extends BlockEntity {
	public static final int DEFAULT_CORE_COLOR = 0xFFFFFFFF;
	public static final int DEFAULT_OUTLINE_COLOR = 0xFFFFFFFF;
	private static final String TAG_CORE_COLOR = "core_color";
	private static final String TAG_OUTLINE_COLOR = "outline_color";
	private static final int CORE_PARTICLE_BASE_COLOR = 0x4F4E4E;
	private static final int OUTLINE_PARTICLE_BASE_COLOR = 0xF1EEEE;
	private static final int MIN_PARTICLE_INTERVAL_TICKS = 12;
	private static final int MAX_PARTICLE_INTERVAL_TICKS = 28;
	private int coreColor = DEFAULT_CORE_COLOR;
	private int outlineColor = DEFAULT_OUTLINE_COLOR;
	private DustParticleOptions coreDust = createDust(DEFAULT_CORE_COLOR, CORE_PARTICLE_BASE_COLOR, 0.75F);
	private DustParticleOptions outlineDust = createDust(DEFAULT_OUTLINE_COLOR, OUTLINE_PARTICLE_BASE_COLOR, 0.85F);
	private long nextParticleTick = -1L;

	public DormantSphereBlockEntity(BlockPos pos, BlockState state) {
		super(TimothatysTrinketsModBlockEntities.DORMANT_SPHERE.get(), pos, state);
	}

	public int getCoreColor() {
		return coreColor;
	}

	public int getOutlineColor() {
		return outlineColor;
	}

	public boolean setCoreColor(int color) {
		int opaqueColor = forceOpaque(color);
		if (coreColor == opaqueColor) {
			return false;
		}
		coreColor = opaqueColor;
		coreDust = createDust(coreColor, CORE_PARTICLE_BASE_COLOR, 0.75F);
		syncToClient();
		return true;
	}

	public boolean setOutlineColor(int color) {
		int opaqueColor = forceOpaque(color);
		if (outlineColor == opaqueColor) {
			return false;
		}
		outlineColor = opaqueColor;
		outlineDust = createDust(outlineColor, OUTLINE_PARTICLE_BASE_COLOR, 0.85F);
		syncToClient();
		return true;
	}

	public void syncToClient() {
		setChanged();
		if (level != null && !level.isClientSide) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, 3);
		}
	}

	public static int getCoreColor(ItemStack stack) {
		return getItemColor(stack, TAG_CORE_COLOR, DEFAULT_CORE_COLOR);
	}

	public static int getOutlineColor(ItemStack stack) {
		return getItemColor(stack, TAG_OUTLINE_COLOR, DEFAULT_OUTLINE_COLOR);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DormantSphereBlockEntity sphere) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		long gameTime = level.getGameTime();
		if (sphere.nextParticleTick < 0L) {
			sphere.scheduleNextParticle(serverLevel, gameTime);
			return;
		}
		if (gameTime >= sphere.nextParticleTick) {
			boolean outline = serverLevel.getRandom().nextBoolean();
			serverLevel.sendParticles(
					outline ? sphere.outlineDust : sphere.coreDust,
					pos.getX() + 0.5D,
					pos.getY() + 0.5D,
					pos.getZ() + 0.5D,
					1,
					outline ? 0.28D : 0.34D,
					outline ? 0.28D : 0.34D,
					outline ? 0.28D : 0.34D,
					outline ? 0.006D : 0.004D
			);
			sphere.scheduleNextParticle(serverLevel, gameTime);
		}
	}

	private void scheduleNextParticle(ServerLevel level, long gameTime) {
		nextParticleTick = gameTime + MIN_PARTICLE_INTERVAL_TICKS
				+ level.getRandom().nextInt(MAX_PARTICLE_INTERVAL_TICKS - MIN_PARTICLE_INTERVAL_TICKS + 1);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt(TAG_CORE_COLOR, coreColor);
		tag.putInt(TAG_OUTLINE_COLOR, outlineColor);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		coreColor = readColor(tag, TAG_CORE_COLOR, DEFAULT_CORE_COLOR);
		outlineColor = readColor(tag, TAG_OUTLINE_COLOR, DEFAULT_OUTLINE_COLOR);
		coreDust = createDust(coreColor, CORE_PARTICLE_BASE_COLOR, 0.75F);
		outlineDust = createDust(outlineColor, OUTLINE_PARTICLE_BASE_COLOR, 0.85F);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	private static int getItemColor(ItemStack stack, String key, int fallback) {
		CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
		return readColor(data.getUnsafe(), key, fallback);
	}

	private static int readColor(CompoundTag tag, String key, int fallback) {
		return tag.contains(key, Tag.TAG_INT) ? forceOpaque(tag.getInt(key)) : fallback;
	}

	private static int forceOpaque(int color) {
		return 0xFF000000 | (color & 0x00FFFFFF);
	}

	private static DustParticleOptions createDust(int tint, int baseColor, float scale) {
		int color = multiplyColors(tint, baseColor);
		return new DustParticleOptions(
				new Vector3f(
						(color >> 16 & 0xFF) / 255.0F,
						(color >> 8 & 0xFF) / 255.0F,
						(color & 0xFF) / 255.0F
				),
				scale
		);
	}

	private static int multiplyColors(int tint, int baseColor) {
		int red = (tint >> 16 & 0xFF) * (baseColor >> 16 & 0xFF) / 255;
		int green = (tint >> 8 & 0xFF) * (baseColor >> 8 & 0xFF) / 255;
		int blue = (tint & 0xFF) * (baseColor & 0xFF) / 255;
		return red << 16 | green << 8 | blue;
	}
}
