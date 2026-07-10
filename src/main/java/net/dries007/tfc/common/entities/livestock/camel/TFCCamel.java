/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.livestock.camel;

import com.mojang.serialization.Dynamic;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.ai.TFCGroundPathNavigation;
import net.dries007.tfc.common.entities.livestock.Age;
import net.dries007.tfc.common.entities.livestock.CommonAnimalData;
import net.dries007.tfc.common.entities.livestock.MammalProperties;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.dries007.tfc.common.entities.livestock.horse.HorseProperties;
import net.dries007.tfc.config.animals.AnimalConfig;
import net.dries007.tfc.config.animals.MammalConfig;
import net.dries007.tfc.util.Helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

public class TFCCamel extends AbstractCamel implements HorseProperties
{
    public static AttributeSupplier.Builder createAttributes()
    {
        return createBaseHorseAttributes()
            .add(Attributes.MAX_HEALTH, 32.0)
            .add(Attributes.MOVEMENT_SPEED, 0.09F)
            .add(Attributes.JUMP_STRENGTH, 0.42F)
            .add(Attributes.STEP_HEIGHT, 1.5);
    }

    private static final CommonAnimalData ANIMAL_DATA = CommonAnimalData.create(TFCCamel.class);
    private static final EntityDataAccessor<Long> PREGNANT_TIME = SynchedEntityData.defineId(TFCCamel.class, EntityDataSerializers.LONG);

    @Nullable private CompoundTag genes;
    private final AnimalConfig config;
    private final MammalConfig mammalConfig;

    public TFCCamel(EntityType<? extends Camel> type, Level level, MammalConfig config)
    {
        super(type, level);
        this.config = config.inner();
        this.mammalConfig = config;
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic)
    {
        return TFCCamelAi.makeBrain(TFCCamelAi.brainProvider().makeBrain(dynamic));
    }

    @Override
    public void createGenes(CompoundTag tag, TFCAnimalProperties maleProperties)
    {
        HorseProperties.super.createGenes(tag, maleProperties);
    }

    @Override
    public void applyGenes(CompoundTag tag, MammalProperties babyProperties)
    {
        HorseProperties.super.applyGenes(tag, babyProperties);
    }

    @Override
    public TagKey<Item> getFoodTag()
    {
        return TFCTags.Items.CAMEL_FOOD;
    }

    @Override
    public void setInLove(@Nullable Player player) {} // nobody could love a camel

    @Override
    public boolean canMate(Animal otherAnimal)
    {
        if (otherAnimal.getClass() != this.getClass()) return false;
        TFCCamel other = (TFCCamel) otherAnimal;
        return this.getGender() != other.getGender()
            && this.isReadyToMate() && other.isReadyToMate()
            && checkExtraBreedConditions(other);
    }

    @Override
    public boolean checkExtraBreedConditions(TFCAnimalProperties otherAnimal)
    {
        if (otherAnimal instanceof TFCCamel otherCamel)
        {
            return vanillaParentingCheck(this) && vanillaParentingCheck(otherCamel);
        }
        return false;
    }

    public boolean vanillaParentingCheck(AbstractHorse camel) {
        return !camel.isVehicle() && !camel.isPassenger();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        InteractionResult result = super.mobInteract(player, hand);
        if (result == InteractionResult.PASS)
        {
            ItemStack stack = player.getItemInHand(hand);
            if (!this.isBaby())
            {
                if (this.isTamed() && player.isSecondaryUseActive())
                {
                    this.openCustomInventoryScreen(player);
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }

                if (this.isVehicle())
                {
                    return InteractionResult.PASS;
                }
            }

            if (!stack.isEmpty())
            {
                InteractionResult res = stack.interactLivingEntity(player, this, hand);
                if (res.consumesAction())
                {
                    return res;
                }

                if (!this.isTamed())
                {
                    this.makeMad();
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }

                final boolean canBeSaddled = !this.isBaby() && !this.isSaddled() && stack.is(Items.SADDLE);
                if (this.isBodyArmorItem(stack) || canBeSaddled)
                {
                    this.openCustomInventoryScreen(player);
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            }

            if (this.isBaby())
            {
                return InteractionResult.PASS;
            }
            else
            {
                if (isTamed() && getOwnerUUID() == null)
                {
                    tameWithName(player);
                }
                if (this.getPassengers().size() < 2)
                {
                    this.doPlayerRide(player);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return result;
    }

    @Override
    public boolean isTamed()
    {
        return getFamiliarity() > TAMED_FAMILIARITY;
    }

    @Override
    protected @Nullable SoundEvent getEatingSound()
    {
        return super.getEatingSound();
    }

    @Override
    protected float getBlockSpeedFactor()
    {
        if ((Helpers.isBlock(level().getBlockState(blockPosition().below()), Tags.Blocks.SANDS)))
        {
            return 1.25F;
        }
        else return Helpers.isBlock(level().getBlockState(blockPosition()), TFCTags.Blocks.ANIMAL_IGNORED_PLANTS) ? 1.0F : super.getBlockSpeedFactor();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData)
    {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (spawnType != MobSpawnType.BREEDING)
        {
            initCommonAnimalData(level, difficulty, spawnType);
        }
        setPregnantTime(-1L);
        return spawnData;
    }

    @Override
    public MammalConfig getMammalConfig()
    {
        return mammalConfig;
    }

    @Override
    public long getPregnantTime()
    {
        return entityData.get(PREGNANT_TIME);
    }

    @Override
    public void setPregnantTime(long day)
    {
        entityData.set(PREGNANT_TIME, day);
    }

    @Override
    public void setGenes(@Nullable CompoundTag tag)
    {
        genes = tag;
    }

    @Override
    public @Nullable CompoundTag getGenes()
    {
        return genes;
    }

    @Override
    public AnimalConfig animalConfig()
    {
        return config;
    }

    @Override
    public CommonAnimalData animalData()
    {
        return ANIMAL_DATA;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        animalData().define(builder);
        builder.define(PREGNANT_TIME, -1L);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt)
    {
        super.addAdditionalSaveData(nbt);
        saveCommonAnimalData(nbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt)
    {
        super.readAdditionalSaveData(nbt);
        readCommonAnimalData(nbt);
    }

    @Override
    public boolean isBaby()
    {
        return getAgeType() == Age.CHILD;
    }

    @Override
    public void setAge(int age)
    {
        super.setAge(0);
    }

    @Override
    public int getAge()
    {
        return isBaby() ? -24000 : 0;
    }

    @Nullable
    @Override
    public TFCCamel getBreedOffspring(ServerLevel level, AgeableMob other)
    {
        final AgeableMob mob = HorseProperties.super.getBreedOffspring(level, other);
        return mob instanceof TFCCamel camel ? camel : null;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data)
    {
        super.onSyncedDataUpdated(data);
        if (ANIMAL_DATA.birthTick().equals(data))
        {
            refreshDimensions();
        }
    }

    @Override
    protected void customServerAiStep()
    {
        // Don't want to call super.customServerAiStep() here because of CamelAi.updateActivity(this)
        ((Brain<TFCCamel>) getBrain()).tick((ServerLevel) level(), this);
        TFCCamelAi.updateActivity(this);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (level().getGameTime() % 20 == 0)
        {
            tickAnimalData();
        }
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src)
    {
        return super.getHurtSound(src);
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return super.getDeathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block)
    {
        super.playStepSound(pos, block);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level)
    {
        return level.getBlockState(pos.below()).is(TFCTags.Blocks.BUSH_PLANTABLE_ON) ? 10.0F : level.getPathfindingCostFromLightLevels(pos);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src)
    {
        return src.is(DamageTypes.CACTUS) || super.isInvulnerableTo(src);
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        return new TFCGroundPathNavigation(this, level);
    }

    @Override
    public boolean isInWall()
    {
        return !level().isClientSide && super.isInWall();
    }

    @Override
    protected void pushEntities()
    {
        if (!level().isClientSide) super.pushEntities();
    }
}
