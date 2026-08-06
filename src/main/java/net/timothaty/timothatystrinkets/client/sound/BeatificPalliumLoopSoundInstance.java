package net.timothaty.timothatystrinkets.client.sound;

import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium.BeatificPalliumData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

final class BeatificPalliumLoopSoundInstance extends AbstractTickableSoundInstance {
	private static final float MAX_VOLUME = 0.10F;

	private final ClientLevel ownerLevel;
	private final BeatificPalliumEntity pallium;
	private final UUID palliumUuid;
	private final int palliumEntityId;

	BeatificPalliumLoopSoundInstance(ClientLevel level, BeatificPalliumEntity pallium) {
		super(TimothatysTrinketsModSounds.BEATIFIC_PALLIUM_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.ownerLevel = level;
		this.pallium = pallium;
		this.palliumUuid = pallium.getUUID();
		this.palliumEntityId = pallium.getId();
		this.volume = 0.0F;
		this.pitch = 1.0F;
		this.looping = true;
		this.delay = 0;
		this.relative = false;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		updatePosition();
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		if (this.isStopped()) {
			BeatificPalliumLoopSoundManager.onInstanceStopped(this.palliumUuid, this);
			return;
		}

		ClientLevel currentLevel = Minecraft.getInstance().level;
		LivingEntity target = this.pallium.getTarget();
		BeatificPalliumEntity.VisualPhase phase = this.pallium.getVisualPhase();
		if (currentLevel == null
				|| currentLevel != this.ownerLevel
				|| this.pallium.isRemoved()
				|| target == null
				|| !target.isAlive()
				|| target.isRemoved()
				|| phase == BeatificPalliumEntity.VisualPhase.FADING
				|| phase == BeatificPalliumEntity.VisualPhase.BURST) {
			forceStop();
			return;
		}

		updatePosition();
		this.volume = phase == BeatificPalliumEntity.VisualPhase.APPEARING
				? MAX_VOLUME * Mth.clamp(
						(float) this.pallium.tickCount / (float) BeatificPalliumData.APPEARANCE_TICKS,
						0.0F,
						1.0F
				)
				: MAX_VOLUME;
	}

	boolean belongsTo(ClientLevel level) {
		return this.ownerLevel == level;
	}

	boolean matchesEntityId(int entityId) {
		return this.palliumEntityId == entityId;
	}

	void forceStop() {
		this.volume = 0.0F;
		if (!this.isStopped())
			this.stop();
		BeatificPalliumLoopSoundManager.onInstanceStopped(this.palliumUuid, this);
		Minecraft.getInstance().getSoundManager().stop(this);
	}

	private void updatePosition() {
		this.x = this.pallium.getX();
		this.y = this.pallium.getY();
		this.z = this.pallium.getZ();
	}
}
