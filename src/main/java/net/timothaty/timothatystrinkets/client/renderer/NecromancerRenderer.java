package net.timothaty.timothatystrinkets.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.client.model.ModelNecromancerModel;

public class NecromancerRenderer extends MobRenderer<NecromancerEntity, ModelNecromancerModel<NecromancerEntity>> {
	public NecromancerRenderer(EntityRendererProvider.Context context) {
		super(context, new ModelNecromancerModel<NecromancerEntity>(context.bakeLayer(ModelNecromancerModel.LAYER_LOCATION)), 0.5f);
		this.addLayer(new NecromancerGlowLayer(this));
	}

	@Override
	public ResourceLocation getTextureLocation(NecromancerEntity entity) {
		return ResourceLocation.parse("timothatys_trinkets:textures/entities/" + entity.getTexture() + ".png");
	}
}
