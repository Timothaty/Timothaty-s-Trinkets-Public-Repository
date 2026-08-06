package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public class OrbitingOrbCurioRenderer {
	private static final float BASE_RADIUS = 0.75F;
	private static final float BASE_HEIGHT = 1F;
	private static final float ORBIT_SPEED = 3.7F;
	private static final float BASE_SCALE = 0.9F;
	private static final float PHASE_TRANSITION_DURATION_TICKS = 10.0F;
	private static final float PHASE_CHANGE_EPSILON = 0.001F;
	private static final int EMISSIVE_LIGHT = LightTexture.FULL_BRIGHT;
	private static final int WHITE_COLOR = 0xFFFFFFFF;

	private static final float VISUAL_COOLDOWN_ALPHA = 0.08F;
	private static final float VISUAL_IDLE_ALPHA = 0.18F;
	private static final float VISUAL_READY_ALPHA = 0.78F;
	private static final float VISUAL_ALPHA_LERP = 0.08F;

	private static final double ORBIT_LAG_LERP = 0.28D;
	private static final double ORBIT_LAG_MULTIPLIER = 2.15D;
	private static final double MAX_ORBIT_LAG = 0.32D;

	private static final int TRAIL_MAX_POINTS = 12;
	private static final int TRAIL_POINT_LIFETIME_TICKS = 12;
	private static final int TRAIL_SAMPLE_INTERVAL_TICKS = 1;
	private static final float TRAIL_COOLDOWN_INTENSITY = 0.28F;
	private static final float TRAIL_REMOTE_INTENSITY = 0.60F;
	private static final double FULL_DISTANCE = 24.0D;
	private static final double RENDER_DISTANCE = 48.0D;
	private static final double UPDATE_DISTANCE = 56.0D;
	private static final double FADE_START_DISTANCE = 40.0D;
	private static final double FULL_DISTANCE_SQR = FULL_DISTANCE * FULL_DISTANCE;
	private static final double RENDER_DISTANCE_SQR = RENDER_DISTANCE * RENDER_DISTANCE;
	private static final double UPDATE_DISTANCE_SQR = UPDATE_DISTANCE * UPDATE_DISTANCE;
	private static final double FADE_START_DISTANCE_SQR = FADE_START_DISTANCE * FADE_START_DISTANCE;
	private static final int STALE_STATE_CLEANUP_INTERVAL_TICKS = 200;

	private static final Map<Integer, EnumMap<OrbType, OrbVisualState>> VISUAL_STATES = new HashMap<>();
	private static final Map<Integer, OrbRenderCache> ORB_RENDER_CACHE = new HashMap<>();
	private static ClientLevel trackedClientLevel;
	private static long lastStateCleanupTick = Long.MIN_VALUE;
	private static VoidSphereModel<LivingEntity> model;

	private OrbitingOrbCurioRenderer() {
	}

	private static VoidSphereModel<LivingEntity> getModel() {
		if (model == null) {
			model = new VoidSphereModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(VoidSphereModel.LAYER_LOCATION));
		}
		return model;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		maintainClientVisualStates();
	}

	@SubscribeEvent
	public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
		maintainClientVisualStates();

		Player player = event.getEntity();
		Minecraft minecraft = Minecraft.getInstance();
		if (player == null || minecraft.level == null) {
			return;
		}

		if (!player.isAlive() || player.isDeadOrDying()) {
			clearVisualState(player.getId());
			return;
		}

		List<OrbType> equippedOrbs = getEquippedOrbTypes(player);
		if (player.isInvisible() || equippedOrbs.isEmpty()) {
			clearVisualState(player.getId());
			return;
		}

		EnumMap<OrbType, OrbVisualState> playerStates = VISUAL_STATES.computeIfAbsent(player.getId(), id -> new EnumMap<>(OrbType.class));
		playerStates.keySet().removeIf(orbType -> !equippedOrbs.contains(orbType));

		VoidSphereModel<LivingEntity> voidSphereModel = getModel();
		float partialTicks = event.getPartialTick();
		Vec3 interpolatedPlayerPosition = player.getPosition(partialTicks);
		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		double cameraOffsetX = cameraPosition.x - interpolatedPlayerPosition.x;
		double cameraOffsetY = cameraPosition.y - interpolatedPlayerPosition.y;
		double cameraOffsetZ = cameraPosition.z - interpolatedPlayerPosition.z;
		TrailRenderContext trailContext = createTrailRenderContext(cameraOffsetX, cameraOffsetY, cameraOffsetZ);

		int equippedOrbCount = equippedOrbs.size();
		for (int orbIndex = 0; orbIndex < equippedOrbCount; orbIndex++) {
			OrbType orbType = equippedOrbs.get(orbIndex);
			OrbVisualState visualState = playerStates.computeIfAbsent(orbType, type -> new OrbVisualState());
			float targetPhase = orbIndex * 360.0F / equippedOrbCount;
			renderOrb(player, orbType, visualState, targetPhase, event.getPoseStack(), event.getMultiBufferSource(), partialTicks, voidSphereModel, trailContext);
		}
	}

	private static List<OrbType> getEquippedOrbTypes(Player player) {
		long now = player.level().getGameTime();
		OrbRenderCache cache = ORB_RENDER_CACHE.get(player.getId());
		if (cache != null && cache.tick == now) {
			return cache.orbs;
		}

		List<OrbType> equippedOrbs = new ArrayList<>();

		CuriosApi.getCuriosInventory(player).ifPresent(curiosInventory -> {
			for (var entry : curiosInventory.getCurios().entrySet()) {
				ICurioStacksHandler curiosStacksHandler = entry.getValue();
				var stacks = curiosStacksHandler.getStacks();

				for (int slot = 0; slot < stacks.getSlots(); slot++) {
					if (!shouldRenderCurioSlot(curiosStacksHandler, slot)) {
						continue;
					}

					OrbType orbType = OrbType.byStack(stacks.getStackInSlot(slot));
					if (orbType != null && !equippedOrbs.contains(orbType)) {
						equippedOrbs.add(orbType);
					}
				}
			}
		});

		equippedOrbs.sort((first, second) -> Integer.compare(first.order, second.order));
		List<OrbType> cachedOrbs = List.copyOf(equippedOrbs);
		ORB_RENDER_CACHE.put(player.getId(), new OrbRenderCache(now, cachedOrbs));
		return cachedOrbs;
	}

	private static boolean shouldRenderCurioSlot(ICurioStacksHandler curiosStacksHandler, int slot) {
		if (!curiosStacksHandler.canToggleRendering()) {
			return true;
		}

		var renders = curiosStacksHandler.getRenders();
		return renders.isEmpty() || slot >= renders.size() || Boolean.TRUE.equals(renders.get(slot));
	}

	private static void renderOrb(
			Player player,
			OrbType orbType,
			OrbVisualState visualState,
			float targetPhase,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			float partialTicks,
			VoidSphereModel<LivingEntity> voidSphereModel,
			TrailRenderContext trailContext
	) {
		resetVisualStateIfTimelineChanged(player, visualState);

		float readinessAlpha = updateReadinessAlpha(player, orbType, visualState);
		Vec3 orbitLagOffset = updateOrbitLagOffset(player, visualState);

		float time = player.tickCount + partialTicks;
		float orbitPhase = updateOrbitPhase(visualState, targetPhase, time);
		float orbitAngle = time * ORBIT_SPEED + orbitPhase;

		double angleRadians = Math.toRadians(orbitAngle);
		float offsetX = (float) (Math.cos(angleRadians) * orbType.radius);
		float offsetZ = (float) (Math.sin(angleRadians) * orbType.radius);
		float height = orbType.height + getPlayerHeightOffset(player);
		double trailX = offsetX + orbitLagOffset.x;
		double trailY = height;
		double trailZ = offsetZ + orbitLagOffset.z;

		if (trailContext.updateHistory) {
			updateTrailHistory(visualState, player.tickCount, trailX, trailY, trailZ);
		} else {
			visualState.clearTrail();
		}

		if (trailContext.renderTrail) {
			OrbTrailRenderer.render(
					poseStack,
					bufferSource,
					visualState.trailPoints,
					trailX,
					trailY,
					trailZ,
					trailContext.localCameraPosition,
					orbType.haloColor,
					trailIntensity(player, readinessAlpha),
					trailContext.lod,
					trailContext.distanceFade
			);
		}

		poseStack.pushPose();
		try {
			poseStack.translate(orbitLagOffset.x + offsetX, height, orbitLagOffset.z + offsetZ);
			renderMainOrb(orbType, orbitAngle, poseStack, bufferSource, voidSphereModel);
		} finally {
			poseStack.popPose();
		}
	}

	private static void renderMainOrb(
			OrbType orbType,
			float orbitAngle,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			VoidSphereModel<LivingEntity> voidSphereModel
	) {
		OrbHaloRenderer.render(poseStack, bufferSource, orbType.haloColor, orbType.scale);
		renderOrbCore(orbType, orbitAngle, poseStack, bufferSource, voidSphereModel);
		renderOrbOutline(orbType, orbitAngle, poseStack, bufferSource, voidSphereModel);
	}

	private static void renderOrbCore(
			OrbType orbType,
			float orbitAngle,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			VoidSphereModel<LivingEntity> voidSphereModel
	) {
		poseStack.pushPose();
		try {
			applyOrbTransform(orbType, orbitAngle, poseStack);

			VertexConsumer coreConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(orbType.texture));
			voidSphereModel.renderCoreToBuffer(poseStack, coreConsumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, WHITE_COLOR);
		} finally {
			poseStack.popPose();
		}
	}

	private static void renderOrbOutline(
			OrbType orbType,
			float orbitAngle,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			VoidSphereModel<LivingEntity> voidSphereModel
	) {
		if (orbType.outlineTexture == null) {
			return;
		}

		poseStack.pushPose();
		try {
			applyOrbTransform(orbType, orbitAngle, poseStack);

			VertexConsumer outlineConsumer = bufferSource.getBuffer(RenderType.entityCutout(orbType.outlineTexture));
			voidSphereModel.renderOutlineToBuffer(poseStack, outlineConsumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, WHITE_COLOR);
		} finally {
			poseStack.popPose();
		}
	}

	private static void applyOrbTransform(OrbType orbType, float orbitAngle, PoseStack poseStack) {
		poseStack.mulPose(Axis.YP.rotationDegrees(orbitAngle * 2.0F * orbType.spinDirection));
		poseStack.mulPose(Axis.XP.rotationDegrees(orbitAngle * 0.5F));
		poseStack.scale(orbType.scale, orbType.scale, orbType.scale);
	}

	private static float updateOrbitPhase(OrbVisualState visualState, float targetPhase, float time) {
		if (!visualState.phaseInitialized) {
			visualState.phaseInitialized = true;
			visualState.phaseStartDegrees = targetPhase;
			visualState.phaseTargetDegrees = targetPhase;
			visualState.phaseTransitionStartTime = time;
			return targetPhase;
		}

		float currentPhase = getCurrentOrbitPhase(visualState, time);
		if (Math.abs(Mth.wrapDegrees(targetPhase - visualState.phaseTargetDegrees)) > PHASE_CHANGE_EPSILON) {
			visualState.phaseStartDegrees = currentPhase;
			visualState.phaseTargetDegrees = currentPhase + Mth.wrapDegrees(targetPhase - currentPhase);
			visualState.phaseTransitionStartTime = time;
		}

		return getCurrentOrbitPhase(visualState, time);
	}

	private static float getCurrentOrbitPhase(OrbVisualState visualState, float time) {
		float progress = Mth.clamp(
				(time - visualState.phaseTransitionStartTime) / PHASE_TRANSITION_DURATION_TICKS,
				0.0F,
				1.0F
		);
		float easedProgress = progress * progress * (3.0F - 2.0F * progress);
		return Mth.lerp(easedProgress, visualState.phaseStartDegrees, visualState.phaseTargetDegrees);
	}

	private static void resetVisualStateIfTimelineChanged(Player player, OrbVisualState visualState) {
		if (visualState.lastSeenTick > player.tickCount) {
			visualState.reset();
		}

		visualState.lastSeenTick = player.tickCount;
	}

	private static float getPlayerHeightOffset(Player player) {
		return player.isCrouching() ? -0.18F : 0.0F;
	}

	private static void clearVisualState(int entityId) {
		VISUAL_STATES.remove(entityId);
		ORB_RENDER_CACHE.remove(entityId);
	}

	private static void maintainClientVisualStates() {
		ClientLevel clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) {
			VISUAL_STATES.clear();
			ORB_RENDER_CACHE.clear();
			trackedClientLevel = null;
			lastStateCleanupTick = Long.MIN_VALUE;
			return;
		}

		if (clientLevel != trackedClientLevel) {
			VISUAL_STATES.clear();
			ORB_RENDER_CACHE.clear();
			trackedClientLevel = clientLevel;
			lastStateCleanupTick = clientLevel.getGameTime();
			return;
		}

		long gameTime = clientLevel.getGameTime();
		if (lastStateCleanupTick == Long.MIN_VALUE
				|| gameTime < lastStateCleanupTick
				|| gameTime - lastStateCleanupTick >= STALE_STATE_CLEANUP_INTERVAL_TICKS) {
			VISUAL_STATES.keySet().removeIf(entityId -> clientLevel.getEntity(entityId) == null);
			ORB_RENDER_CACHE.keySet().removeIf(entityId -> clientLevel.getEntity(entityId) == null);
			lastStateCleanupTick = gameTime;
		}
	}

	private static TrailRenderContext createTrailRenderContext(double cameraX, double cameraY, double cameraZ) {
		double distanceSquared = cameraX * cameraX + cameraY * cameraY + cameraZ * cameraZ;
		boolean updateHistory = distanceSquared <= UPDATE_DISTANCE_SQR;
		boolean renderTrail = distanceSquared < RENDER_DISTANCE_SQR;
		OrbTrailRenderer.Lod lod = distanceSquared <= FULL_DISTANCE_SQR
				? OrbTrailRenderer.Lod.FULL
				: OrbTrailRenderer.Lod.REDUCED;

		float distanceFade = 1.0F;
		if (distanceSquared > FADE_START_DISTANCE_SQR) {
			double distance = Math.sqrt(distanceSquared);
			distanceFade = Mth.clamp((float) ((RENDER_DISTANCE - distance) / (RENDER_DISTANCE - FADE_START_DISTANCE)), 0.0F, 1.0F);
		}

		return new TrailRenderContext(lod, updateHistory, renderTrail, distanceFade, new Vec3(cameraX, cameraY, cameraZ));
	}

	private static float updateReadinessAlpha(Player player, OrbType orbType, OrbVisualState visualState) {
		float targetAlpha = VISUAL_IDLE_ALPHA;

		if (player == Minecraft.getInstance().player) {
			boolean ready = !player.getCooldowns().isOnCooldown(orbType.cooldownItem);
			targetAlpha = ready ? VISUAL_READY_ALPHA : VISUAL_COOLDOWN_ALPHA;
		}

		visualState.readinessAlpha = Mth.lerp(VISUAL_ALPHA_LERP, visualState.readinessAlpha, targetAlpha);
		return visualState.readinessAlpha;
	}

	private static Vec3 updateOrbitLagOffset(Player player, OrbVisualState visualState) {
		Vec3 velocity = player.getDeltaMovement();
		Vec3 targetOffset = new Vec3(-velocity.x * ORBIT_LAG_MULTIPLIER, 0.0D, -velocity.z * ORBIT_LAG_MULTIPLIER);

		if (targetOffset.lengthSqr() > MAX_ORBIT_LAG * MAX_ORBIT_LAG) {
			targetOffset = targetOffset.normalize().scale(MAX_ORBIT_LAG);
		}

		visualState.smoothOrbitLagOffset = visualState.smoothOrbitLagOffset.lerp(targetOffset, ORBIT_LAG_LERP);
		return visualState.smoothOrbitLagOffset;
	}

	private static void updateTrailHistory(OrbVisualState visualState, int currentTick, double x, double y, double z) {
		visualState.trailPoints.removeIf(point -> {
			int age = currentTick - point.createdTick;
			return age < 0 || age > TRAIL_POINT_LIFETIME_TICKS;
		});

		if (currentTick - visualState.lastTrailSampleTick < TRAIL_SAMPLE_INTERVAL_TICKS) {
			return;
		}

		visualState.lastTrailSampleTick = currentTick;
		visualState.trailPoints.add(new OrbTrailPoint(x, y, z, currentTick));
		while (visualState.trailPoints.size() > TRAIL_MAX_POINTS) {
			visualState.trailPoints.remove(0);
		}
	}

	private static float trailIntensity(Player player, float readinessAlpha) {
		if (player != Minecraft.getInstance().player) {
			return TRAIL_REMOTE_INTENSITY;
		}

		float readyProgress = Mth.clamp(
				(readinessAlpha - VISUAL_COOLDOWN_ALPHA) / (VISUAL_READY_ALPHA - VISUAL_COOLDOWN_ALPHA),
				0.0F,
				1.0F
		);
		return Mth.lerp(readyProgress, TRAIL_COOLDOWN_INTENSITY, 1.0F);
	}

	private static ResourceLocation texture(String path) {
		return ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/" + path + ".png");
	}

	public enum OrbType {
		VOID(
				0,
				texture("block/void_sphere_cube"),
				texture("block/outline_void_sphere"),
				TimothatysTrinketsModItems.VOID_SPHERE.get(),
				BASE_RADIUS,
				BASE_HEIGHT,
				BASE_SCALE,
				1.0F,
				0x4C2390
		),
		ECHO(
				1,
				texture("block/echo_sphere_cube"),
				texture("block/outline_echo_sphere"),
				TimothatysTrinketsModItems.ECHO_SPHERE.get(),
				BASE_RADIUS,
				BASE_HEIGHT + 0.22F,
				BASE_SCALE,
				-1.0F,
				0x29DFEB
		),
		FIRE(
				2,
				texture("block/fire_sphere_cube"),
				texture("block/outline_fire_sphere"),
				TimothatysTrinketsModItems.FIRE_SPHERE.get(),
				BASE_RADIUS,
				BASE_HEIGHT + 0.10F,
				BASE_SCALE,
				1.0F,
				0xFF9824
		),
		VENOM(
				3,
				texture("block/venomous_sphere_cube"),
				texture("block/outline_venomous_sphere"),
				TimothatysTrinketsModItems.VENOM_SPHERE.get(),
				BASE_RADIUS,
				BASE_HEIGHT + 0.32F,
				BASE_SCALE,
				-1.0F,
				0x61CE16
		);

		private final int order;
		private final ResourceLocation texture;
		private final ResourceLocation outlineTexture;
		private final Item cooldownItem;
		private final float radius;
		private final float height;
		private final float scale;
		private final float spinDirection;
		private final int haloColor;

		OrbType(int order, ResourceLocation texture, ResourceLocation outlineTexture, Item cooldownItem, float radius, float height, float scale, float spinDirection, int haloColor) {
			this.order = order;
			this.texture = texture;
			this.outlineTexture = outlineTexture;
			this.cooldownItem = cooldownItem;
			this.radius = radius;
			this.height = height;
			this.scale = scale;
			this.spinDirection = spinDirection;
			this.haloColor = haloColor;
		}

		private static OrbType byStack(ItemStack stack) {
			if (stack.isEmpty()) {
				return null;
			}

			for (OrbType orbType : values()) {
				if (stack.is(orbType.cooldownItem)) {
					return orbType;
				}
			}

			return null;
		}
	}

	private static class OrbVisualState {
		private float readinessAlpha;
		private boolean phaseInitialized;
		private float phaseStartDegrees;
		private float phaseTargetDegrees;
		private float phaseTransitionStartTime;
		private int lastTrailSampleTick = Integer.MIN_VALUE / 2;
		private int lastSeenTick = -1;
		private Vec3 smoothOrbitLagOffset = Vec3.ZERO;
		private final List<OrbTrailPoint> trailPoints = new ArrayList<>(TRAIL_MAX_POINTS);

		private void reset() {
			readinessAlpha = 0.0F;
			phaseInitialized = false;
			phaseStartDegrees = 0.0F;
			phaseTargetDegrees = 0.0F;
			phaseTransitionStartTime = 0.0F;
			lastSeenTick = -1;
			smoothOrbitLagOffset = Vec3.ZERO;
			clearTrail();
		}

		private void clearTrail() {
			lastTrailSampleTick = Integer.MIN_VALUE / 2;
			trailPoints.clear();
		}
	}

	static record OrbTrailPoint(double x, double y, double z, int createdTick) {
	}

	private record TrailRenderContext(
			OrbTrailRenderer.Lod lod,
			boolean updateHistory,
			boolean renderTrail,
			float distanceFade,
			Vec3 localCameraPosition
	) {
	}

	private record OrbRenderCache(long tick, List<OrbType> orbs) {
	}

	public static class VoidSphereModel<T extends LivingEntity> extends EntityModel<T> {
		public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
				ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "void_sphere"),
				"main"
		);

		private static final float CORE_HALF_SIZE = 2.0F;
		private static final float CORE_SIZE = CORE_HALF_SIZE * 2.0F;
		private static final float OUTLINE_HALF_SIZE = CORE_HALF_SIZE + 0.2F;
		private static final float OUTLINE_SIZE = OUTLINE_HALF_SIZE * 2.0F;

		private final ModelPart root;
		private final ModelPart core;
		private final ModelPart outline;

		public VoidSphereModel(ModelPart root) {
			super(RenderType::entityCutoutNoCull);
			this.root = root;
			this.core = root.getChild("core");
			this.outline = root.getChild("outline");
		}

		public static LayerDefinition createBodyLayer() {
			MeshDefinition meshDefinition = new MeshDefinition();
			PartDefinition root = meshDefinition.getRoot();

			root.addOrReplaceChild(
					"core",
					CubeListBuilder.create()
							.texOffs(0, 0)
							.addBox(
									-CORE_HALF_SIZE,
									-CORE_HALF_SIZE,
									-CORE_HALF_SIZE,
									CORE_SIZE,
									CORE_SIZE,
									CORE_SIZE,
									CubeDeformation.NONE
							),
					PartPose.ZERO
			);

			root.addOrReplaceChild(
					"outline",
					CubeListBuilder.create()
							.texOffs(0, 0)
							.addBox(
									OUTLINE_HALF_SIZE,
									OUTLINE_HALF_SIZE,
									OUTLINE_HALF_SIZE,
									-OUTLINE_SIZE,
									-OUTLINE_SIZE,
									-OUTLINE_SIZE,
									CubeDeformation.NONE
							),
					PartPose.ZERO
			);

			return LayerDefinition.create(meshDefinition, 16, 16);
		}

		@Override
		public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		}

		@Override
		public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
			this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		}

		public void renderCoreToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
			this.core.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		}

		public void renderOutlineToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
			this.outline.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		}
	}
}
