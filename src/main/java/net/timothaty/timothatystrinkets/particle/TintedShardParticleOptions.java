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
import net.minecraft.util.Mth;

import org.joml.Vector3f;

public final class TintedShardParticleOptions extends ScalableParticleOptionsBase {
	public static final MapCodec<TintedShardParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(TintedShardParticleOptions::color),
			SCALE.fieldOf("scale").forGetter(TintedShardParticleOptions::scale)
	).apply(instance, TintedShardParticleOptions::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, TintedShardParticleOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VECTOR3F,
			TintedShardParticleOptions::color,
			ByteBufCodecs.FLOAT,
			TintedShardParticleOptions::scale,
			TintedShardParticleOptions::new
	);

	private final Vector3f color;

	public TintedShardParticleOptions(Vector3f color, float scale) {
		super(Math.max(0.0F, scale));
		Vector3f safeColor = color != null ? color : new Vector3f(1.0F, 1.0F, 1.0F);
		this.color = new Vector3f(
				Mth.clamp(safeColor.x(), 0.0F, 1.0F),
				Mth.clamp(safeColor.y(), 0.0F, 1.0F),
				Mth.clamp(safeColor.z(), 0.0F, 1.0F)
		);
	}

	public Vector3f color() {
		return new Vector3f(this.color);
	}

	public float scale() {
		return this.getScale();
	}

	@Override
	public ParticleType<TintedShardParticleOptions> getType() {
		return TimothatysTrinketsModParticleTypes.TINTED_SHARD.get();
	}
}
