package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.LivingEntity;

public class VoidMarkParticleData {
	private static final double ATTACH_MAGIC = 0.041527D;
	private static final double MARKER_EPSILON = 0.0001D;
	private static final double ENTITY_ID_EPSILON = 0.25D;

	public static double encodeEntityId(LivingEntity entity) {
		return entity.getId();
	}

	public static double attachMagic() {
		return ATTACH_MAGIC;
	}

	public static int decodeEntityId(double encodedEntityId, double marker) {
		if (Math.abs(marker - ATTACH_MAGIC) > MARKER_EPSILON)
			return -1;

		int entityId = (int) Math.round(encodedEntityId);
		if (entityId < 0)
			return -1;

		return Math.abs(encodedEntityId - entityId) <= ENTITY_ID_EPSILON ? entityId : -1;
	}
}
