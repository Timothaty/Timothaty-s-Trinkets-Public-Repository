package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundSource;

@OnlyIn(Dist.CLIENT)
public class ToksikParticle extends TextureSheetParticle {
	private static final float BASE_GRAVITY = 0.015F;
	private static final float SHARD_GRAVITY = 0.015F;
	private static final int MIN_LIFETIME = 24;
	private static final int RANDOM_LIFETIME = 16;
	private static final int MIN_SHARD_LIFETIME = 7;
	private static final int RANDOM_SHARD_LIFETIME = 5;
	private static final float IMPACT_SOUND_CHANCE = 0.05F;
	private static final int MIN_BURST_SHARDS = 2;
	private static final int RANDOM_BURST_SHARDS = 2;
	private static final double LOCAL_PLAYER_BOUNDS_INFLATE = 0.25D;

	private final SpriteSet spriteSet;
	private final boolean shard;
	private final boolean belongsToLocalPlayer;
	private boolean playedImpactSound;
	private boolean burst;

	public static ToksikParticleProvider provider(SpriteSet spriteSet) {
		return new ToksikParticleProvider(spriteSet);
	}

	public static class ToksikParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public ToksikParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new ToksikParticle(
					worldIn,
					x,
					y,
					z,
					xSpeed,
					ySpeed,
					zSpeed,
					this.spriteSet,
					false,
					belongsToLocalPlayer(worldIn, x, y, z)
			);
		}
	}

	protected ToksikParticle(
			ClientLevel world,
			double x,
			double y,
			double z,
			double vx,
			double vy,
			double vz,
			SpriteSet spriteSet,
			boolean shard,
			boolean belongsToLocalPlayer
	) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.shard = shard;
		this.belongsToLocalPlayer = belongsToLocalPlayer;
		this.setSize(shard ? 0.045F : 0.08F, shard ? 0.045F : 0.08F);
		this.lifetime = shard
				? MIN_SHARD_LIFETIME + this.random.nextInt(RANDOM_SHARD_LIFETIME + 1)
				: MIN_LIFETIME + this.random.nextInt(RANDOM_LIFETIME + 1);
		this.gravity = shard ? SHARD_GRAVITY : BASE_GRAVITY;
		this.hasPhysics = true;
		this.friction = shard ? 0.84F : 0.90F;

		double sideChaos = shard ? 0.0D : 0.045D;
		this.xd = vx * (shard ? 1.0D : 1.25D) + randomBetween(-sideChaos, sideChaos);
		this.yd = shard ? vy : vy * 0.65D - 0.035D - this.random.nextDouble() * 0.055D;
		this.zd = vz * (shard ? 1.0D : 1.25D) + randomBetween(-sideChaos, sideChaos);
		this.quadSize *= shard ? 0.34F + this.random.nextFloat() * 0.20F : 0.70F + this.random.nextFloat() * 0.45F;
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		if (this.belongsToLocalPlayer
				&& Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON)
			return;

		super.render(buffer, camera, partialTicks);
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ >= this.lifetime) {
			burstAndRemove(false);
			return;
		}

		this.yd -= this.gravity;
		this.move(this.xd, this.yd, this.zd);
		this.xd *= this.friction;
		this.yd *= this.friction;
		this.zd *= this.friction;

		float progress = Math.min(1.0F, this.age / (float) this.lifetime);
		this.setAlpha(this.shard ? 1.0F - progress : 1.0F - progress * 0.25F);
		this.pickSprite(this.spriteSet);

		if (this.onGround) {
			playImpactSound();
			burstAndRemove(true);
		}
	}

	private void burstAndRemove(boolean fromImpact) {
		if (!this.shard && !this.burst) {
			this.burst = true;
			spawnToxicShards(fromImpact);
		}
		this.remove();
	}

	private void spawnToxicShards(boolean fromImpact) {
		int count = MIN_BURST_SHARDS + this.random.nextInt(RANDOM_BURST_SHARDS + 1);
		for (int i = 0; i < count; i++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0D;
			double speed = 0.035D + this.random.nextDouble() * (fromImpact ? 0.075D : 0.045D);
			double vx = Math.cos(angle) * speed;
			double vz = Math.sin(angle) * speed;
			double vy = (fromImpact ? 0.035D : 0.0D) + this.random.nextDouble() * 0.045D;
			Minecraft.getInstance().particleEngine.add(new ToksikParticle(
					this.level,
					this.x,
					this.y + 0.025D,
					this.z,
					vx,
					vy,
					vz,
					this.spriteSet,
					true,
					this.belongsToLocalPlayer
			));
		}
	}

	private static boolean belongsToLocalPlayer(ClientLevel level, double x, double y, double z) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null
				|| minecraft.player.level() != level
				|| !minecraft.player.hasEffect(TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY))
			return false;

		return minecraft.player.getBoundingBox()
				.inflate(LOCAL_PLAYER_BOUNDS_INFLATE)
				.contains(x, y, z);
	}

	private void playImpactSound() {
		if (this.playedImpactSound || this.random.nextFloat() > IMPACT_SOUND_CHANCE)
			return;

		this.playedImpactSound = true;
		this.level.playLocalSound(
				this.x,
				this.y,
				this.z,
				TimothatysTrinketsModSounds.TOXIC_DROPLET.get(),
				SoundSource.AMBIENT,
				0.04F,
				1.0F,
				false
		);
	}

	private double randomBetween(double min, double max) {
		return min + this.random.nextDouble() * (max - min);
	}
}
