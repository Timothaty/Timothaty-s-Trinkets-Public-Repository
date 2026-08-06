package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.LivingEntity;

public final class MoltenBaneMarkParticleData {
	public static final int VISUAL_OVERLAY_STAGE = 99;
	private static final double ATTACH_MAGIC = 0.073419D;
	private static final double MARKER_EPSILON = 0.0001D;
	private static final double ENTITY_ID_EPSILON = 0.25D;

	private MoltenBaneMarkParticleData() {
	}

	public static double encodeEntityId(LivingEntity entity) {
		return entity.getId();
	}

	public static double attachMagic() {
		return ATTACH_MAGIC;
	}

	public static double encodeStage(int stage) {
		if (stage == VISUAL_OVERLAY_STAGE)
			return VISUAL_OVERLAY_STAGE;
		return clampStage(stage);
	}

	public static int decodeEntityId(double encodedEntityId, double marker) {
		if (Math.abs(marker - ATTACH_MAGIC) > MARKER_EPSILON)
			return -1;

		int entityId = (int) Math.round(encodedEntityId);
		if (entityId < 0)
			return -1;

		return Math.abs(encodedEntityId - entityId) <= ENTITY_ID_EPSILON ? entityId : -1;
	}

	public static int decodeStage(double encodedStage) {
		int stage = (int) Math.round(encodedStage);
		if (stage == VISUAL_OVERLAY_STAGE)
			return VISUAL_OVERLAY_STAGE;
		return clampStage(stage);
	}

	private static int clampStage(int stage) {
		return Math.max(1, Math.min(4, stage));
	}
}