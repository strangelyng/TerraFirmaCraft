package net.dries007.tfc.mixin;

import net.dries007.tfc.common.blocks.GroundcoverBlockType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bat.class)
public class BatMixin extends AmbientCreature {
    protected BatMixin(EntityType<? extends AmbientCreature> p_27403_, Level p_27404_) {
        super(p_27403_, p_27404_);
    }

    @Unique
    public int guanoTime = this.random.nextInt(ICalendar.CALENDAR_TICKS_IN_HOUR*6) + ICalendar.CALENDAR_TICKS_IN_HOUR*6;

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    public void tfc$customServerAiStep(CallbackInfo ci)
    {
        if (!this.level().isClientSide && this.isAlive() && !this.isBaby() && --this.guanoTime <= 0)
        {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation(TFCBlocks.GROUNDCOVER.get(GroundcoverBlockType.GUANO).asItem());
            this.gameEvent(GameEvent.ENTITY_PLACE);
            this.guanoTime = this.random.nextInt(ICalendar.CALENDAR_TICKS_IN_HOUR*6) + ICalendar.CALENDAR_TICKS_IN_HOUR*6;
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void tfc$readAdditionalSaveData(CompoundTag compound, CallbackInfo ci)
    {
        if (compound.contains("GuanoDropTime"))
        {
            this.guanoTime = compound.getInt("GuanoDropTime");
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void tfc$addAdditionalSaveData(CompoundTag compound, CallbackInfo ci)
    {
        compound.putInt("GuanoDropTime", this.guanoTime);
    }
}
