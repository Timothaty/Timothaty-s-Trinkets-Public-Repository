package net.timothaty.timothatystrinkets.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import org.joml.Vector3f;

public final class RitualSmokeParticleOptions implements ParticleOptions {
	public static final MapCodec<RitualSmokeParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.VECTOR3F.fieldOf("target_offset").forGetter(RitualSmokeParticleOptions::targetOffset)
	).apply(instance, RitualSmokeParticleOptions::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, RitualSmokeParticleOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VECTOR3F,
			RitualSmokeParticleOptions::targetOffset,
			RitualSmokeParticleOptions::new
	);

	private final Vector3f targetOffset;

	public RitualSmokeParticleOptions(Vector3f targetOffset) {
		this.targetOffset = new Vector3f(targetOffset);
	}

	public Vector3f targetOffset() {
		return new Vector3f(this.targetOffset);
	}

	@Override
	public ParticleType<?> getType() {
		return TimothatysTrinketsModParticleTypes.RITUAL_SMOKE.get();
	}
}
