package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
public class ShockwaveParticle extends TextureSheetParticle {
	private static final int DECAL_LIFETIME_TICKS = 28;
	private static final int SHARD_LIFETIME_TICKS = 24;
	private static final float MIN_RENDER_RADIUS = 0.35F;
	private static final float Y_OFFSET = 0.00025F;

	public static ShockwaveParticleProvider provider(SpriteSet spriteSet) {
		return new ShockwaveParticleProvider(spriteSet);
	}

	public static class ShockwaveParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public ShockwaveParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new ShockwaveParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;
	private final boolean shardMode;
	private final double startX;
	private final double startY;
	private final double startZ;
	private final double targetX;
	private final double targetZ;
	private final float targetRadius;
	private float renderRadius = MIN_RENDER_RADIUS;
	private float renderRadiusOld = MIN_RENDER_RADIUS;

	protected ShockwaveParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y + Y_OFFSET, z);
		this.spriteSet = spriteSet;
		this.shardMode = vy > 0.0D;
		this.startX = x;
		this.startY = y + Y_OFFSET;
		this.startZ = z;
		this.targetX = x + vx;
		this.targetZ = z + vz;
		this.targetRadius = Math.max(MIN_RENDER_RADIUS, this.shardMode ? (float) vy : (float) vx);
		this.lifetime = this.shardMode ? SHARD_LIFETIME_TICKS : DECAL_LIFETIME_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.alpha = 0.0F;
		this.quadSize = this.shardMode ? Mth.clamp(this.targetRadius * 0.055F, 0.28F, 0.85F) : 0.01F;
		this.setSize(0.01F, 0.01F);
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		this.renderRadiusOld = this.renderRadius;

		this.age++;
		if (this.age >= this.lifetime) {
			this.remove();
			return;
		}

		float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
		float eased = 1.0F - (1.0F - progress) * (1.0F - progress);

		if (this.shardMode) {
			this.setPos(
					Mth.lerp(eased, this.startX, this.targetX),
					this.startY,
					Mth.lerp(eased, this.startZ, this.targetZ)
			);
			this.quadSize = Mth.clamp(this.targetRadius * 0.055F, 0.28F, 0.85F) * (1.0F - progress * 0.18F);
		} else {
			this.renderRadius = Mth.lerp(eased, MIN_RENDER_RADIUS, this.targetRadius);
		}

		float fadeIn = Mth.clamp(progress / 0.14F, 0.0F, 1.0F);
		float hold = progress < 0.42F ? 1.0F : Mth.clamp((1.0F - progress) / 0.58F, 0.0F, 1.0F);
		this.alpha = fadeIn * hold;
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		if (this.shardMode) {
			super.render(buffer, camera, partialTicks);
			return;
		}

		Vec3 cameraPos = camera.getPosition();

		float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
		float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
		float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

		float radius = Mth.lerp(partialTicks, this.renderRadiusOld, this.renderRadius);
		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();
		int light = this.getLightColor(partialTicks);

		addGroundVertex(buffer, particleX, particleY, particleZ, -radius, -radius, u0, v0, light, 1.0F);
		addGroundVertex(buffer, particleX, particleY, particleZ, -radius, radius, u0, v1, light, 1.0F);
		addGroundVertex(buffer, particleX, particleY, particleZ, radius, radius, u1, v1, light, 1.0F);
		addGroundVertex(buffer, particleX, particleY, particleZ, radius, -radius, u1, v0, light, 1.0F);

		addGroundVertex(buffer, particleX, particleY, particleZ, radius, -radius, u1, v0, light, -1.0F);
		addGroundVertex(buffer, particleX, particleY, particleZ, radius, radius, u1, v1, light, -1.0F);
		addGroundVertex(buffer, particleX, particleY, particleZ, -radius, radius, u0, v1, light, -1.0F);
		addGroundVertex(buffer, particleX, particleY, particleZ, -radius, -radius, u0, v0, light, -1.0F);
	}

	private void addGroundVertex(VertexConsumer buffer, float centerX, float centerY, float centerZ, float localX, float localZ, float u, float v, int light, float normalY) {
		buffer.addVertex(centerX + localX, centerY, centerZ + localZ)
				.setUv(u, v)
				.setColor(this.rCol, this.gCol, this.bCol, this.alpha)
				.setLight(light)
				.setNormal(0.0F, normalY, 0.0F);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}
}
