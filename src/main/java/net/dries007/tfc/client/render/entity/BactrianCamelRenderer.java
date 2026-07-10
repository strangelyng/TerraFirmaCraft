package net.dries007.tfc.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.camel.Camel;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.client.model.entity.HierarchicalAnimatedModel;
import net.dries007.tfc.common.entities.livestock.Age;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;

public class BactrianCamelRenderer<T extends Camel, M extends HierarchicalAnimatedModel<T>> extends MobRenderer<T, M>
{
    private final ResourceLocation young;
    private final ResourceLocation old;

    public BactrianCamelRenderer(EntityRendererProvider.Context ctx, M model, float shadow)
    {
        super(ctx, model, shadow);
        this.young = RenderHelpers.animalTexture("bactrian_camel_young");
        this.old = RenderHelpers.animalTexture("bactrian_camel_old");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        if (entity instanceof TFCAnimalProperties animal) {
            return animal.getAgeType() == Age.OLD ? old : young;
        }
        else return RenderHelpers.animalTexture("bactrian_camel_young");
    }
}
