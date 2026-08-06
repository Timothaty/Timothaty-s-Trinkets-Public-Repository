package net.timothaty.timothatystrinkets.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;

public class DebtlordRenderer extends MobRenderer<DebtlordEntity, ModelDebtlord<DebtlordEntity>> {
	public DebtlordRenderer(EntityRendererProvider.Context context) {
		super(context, new ModelDebtlord<DebtlordEntity>(context.bakeLayer(ModelDebtlord.LAYER_LOCATION)), 0.5f);
		this.addLayer(new DebtlordDeathBloodLayer(this));
		this.addLayer(new DebtlordClawTrailLayer(this));
		this.addLayer(new DebtlordChainLayer(this));
		this.addLayer(new DebtlordCastVfxLayer(this));
		this.addLayer(new DebtlordFingerOfDeathLayer(this));
	}

	@Override
	public ResourceLocation getTextureLocation(DebtlordEntity entity) {
		return ResourceLocation.parse("timothatys_trinkets:textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	protected RenderType getRenderType(DebtlordEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
		if (entity.getRenderAlpha(0.0F) < 0.999F)
			return RenderType.entityTranslucent(getTextureLocation(entity));
		return super.getRenderType(entity, bodyVisible, translucent, glowing);
	}

	@Override
	protected float getFlipDegrees(DebtlordEntity entity) {
		return 0.0F;
	}
}
