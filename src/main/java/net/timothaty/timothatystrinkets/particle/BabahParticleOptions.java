package net.timothaty.timothatystrinkets.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import org.joml.Vector3f;

public final class BabahParticleOptions extends ScalableParticleOptionsBase {
	public static final MapCodec<BabahParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(BabahParticleOptions::getColor),
			SCALE.fieldOf("scale").forGetter(BabahParticleOptions::getScale)
	).apply(instance, BabahParticleOptions::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, BabahParticleOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VECTOR3F,
			BabahParticleOptions::getColor,
			ByteBufCodecs.FLOAT,
			BabahParticleOptions::getScale,
			BabahParticleOptions::new
	);

	private final Vector3f color;

	public BabahParticleOptions(Vector3f color, float scale) {
		super(scale);
		this.color = new Vector3f(color);
	}

	public Vector3f getColor() {
		return new Vector3f(this.color);
	}

	@Override
	public ParticleType<BabahParticleOptions> getType() {
		return TimothatysTrinketsModParticleTypes.BABAH.get();
	}
}
