package net.timothaty.timothatystrinkets.client.particle;

import net.timothaty.timothatystrinkets.particle.BabahParticleOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class BabahParticle extends FireworkParticles.OverlayParticle {
	private final float scale;

	private BabahParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			BabahParticleOptions options,
			SpriteSet sprites
	) {
		super(level, x, y, z);
		Vector3f color = options.getColor();
		setColor(color.x(), color.y(), color.z());
		this.scale = options.getScale();
		pickSprite(sprites);
	}

	@Override
	public float getQuadSize(float partialTick) {
		return super.getQuadSize(partialTick) * this.scale;
	}

	public static ParticleProvider<BabahParticleOptions> provider(SpriteSet sprites) {
		return (options, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
				new BabahParticle(level, x, y, z, options, sprites);
	}
}
