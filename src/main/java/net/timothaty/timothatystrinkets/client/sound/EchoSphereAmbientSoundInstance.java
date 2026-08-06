package net.timothaty.timothatystrinkets.client.sound;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

final class EchoSphereAmbientSoundInstance extends AbstractTickableSoundInstance {
	private final ClientLevel ownerLevel;
	private final ResourceKey<Level> dimension;
	private final BlockPos blockPos;
	private final GlobalPos managerKey;

	EchoSphereAmbientSoundInstance(ClientLevel level, BlockPos pos, GlobalPos managerKey) {
		super(TimothatysTrinketsModSounds.ECHO_ORB_BLOCK_LOOP.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
		this.ownerLevel = level;
		this.dimension = level.dimension();
		this.blockPos = pos.immutable();
		this.managerKey = managerKey;
		this.volume = 0.65F;
		this.pitch = 1.0F;
		this.looping = true;
		this.delay = 0;
		this.relative = false;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		this.x = pos.getX() + 0.5D;
		this.y = pos.getY() + 0.5D;
		this.z = pos.getZ() + 0.5D;
	}

	@Override
	public void tick() {
		if (this.isStopped()) {
			EchoSphereAmbientSoundManager.onInstanceStopped(this.managerKey, this);
			return;
		}

		ClientLevel currentLevel = Minecraft.getInstance().level;
		if (currentLevel == null
				|| currentLevel != this.ownerLevel
				|| !currentLevel.dimension().equals(this.dimension)
				|| !currentLevel.hasChunkAt(this.blockPos)
				|| !currentLevel.getBlockState(this.blockPos).is(TimothatysTrinketsModBlocks.ECHO_SPHERE.get())) {
			forceStop();
		}
	}

	boolean belongsTo(ClientLevel level) {
		return this.ownerLevel == level;
	}

	void forceStop() {
		if (!this.isStopped()) {
			this.volume = 0.0F;
			this.stop();
		}
		EchoSphereAmbientSoundManager.onInstanceStopped(this.managerKey, this);
	}
}
