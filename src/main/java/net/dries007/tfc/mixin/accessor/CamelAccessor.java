package net.dries007.tfc.mixin.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camel.class)
public interface CamelAccessor
{
    @Invoker("getBodyAnchorAnimationYOffset")
    double invoke$getBodyAnchorAnimationYOffset(boolean firstPassenger, float partialTick, EntityDimensions dimensions, float scale);
}
