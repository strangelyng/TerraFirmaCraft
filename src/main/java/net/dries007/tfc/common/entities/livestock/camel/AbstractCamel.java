package net.dries007.tfc.common.entities.livestock.camel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.entities.Temptable;
import net.dries007.tfc.common.entities.livestock.MammalProperties;

public abstract class AbstractCamel extends Camel implements MammalProperties, Temptable
{
    protected AbstractCamel(EntityType<? extends Camel> entityType, Level level)
    {
        super(entityType, level);
    }

    @Override
    public @Nullable AbstractCamel getBreedOffspring(ServerLevel level, AgeableMob other)
    {
        final AgeableMob mob = MammalProperties.super.getBreedOffspring(level, other);
        return mob instanceof AbstractCamel camel ? camel : null;
    }

    @Override
    public boolean isFood(ItemStack stack)
    {
        return MammalProperties.super.isFood(stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        InteractionResult result = MammalProperties.super.mobInteract(player, hand);
        return result == InteractionResult.PASS ? super.mobInteract(player, hand) : result;
    }

    @Override
    public EntityType<?> getEntityTypeForBaby()
    {
        return MammalProperties.super.getEntityTypeForBaby();
    }
}
