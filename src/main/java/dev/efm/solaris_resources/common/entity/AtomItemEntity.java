package dev.efm.solaris_resources.common.entity;

import dev.efm.solaris_resources.common.registration.EntityTypeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AtomItemEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(AtomItemEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final int DEFAULT_LIFESPAN = 6000;
    private static final double MERGE_RANGE = 0.5D;

    public final float bobOffs;
    private int age;
    private int lifespan = DEFAULT_LIFESPAN;

    public AtomItemEntity(EntityType<? extends AtomItemEntity> type, Level level) {
        super(type, level);
        this.bobOffs = this.random.nextFloat() * (float) Math.PI * 2.0F;
        this.setYRot(this.random.nextFloat() * 360.0F);
    }

    public AtomItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(EntityTypeRegistries.ATOM_ITEM.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.lifespan = stack.getEntityLifespan(level);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(DATA_ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.getEntityData().set(DATA_ITEM, stack);
    }

    public int getAge() {
        return this.age;
    }

    public float getSpin(float partialTicks) {
        return ((float) this.age + partialTicks) / 20.0F + this.bobOffs;
    }

    @Override
    public void tick() {
        if (this.getItem().isEmpty()) {
            this.discard();
            return;
        }

        super.tick();

        if (this.isInLava() || this.isOnFire()) {
            this.discard();
            return;
        }

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        Vec3 vec3 = this.getDeltaMovement();
        float f = this.getEyeHeight() - 0.11111111F;
        if (this.isInWater() && this.getFluidHeight(FluidTags.WATER) > (double) f) {
            this.setUnderwaterMovement();
        } else if (this.isInLava() && this.getFluidHeight(FluidTags.LAVA) > (double) f) {
            this.setUnderLavaMovement();
        } else if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        if (this.level().isClientSide) {
            this.noPhysics = false;
        } else {
            this.noPhysics = !this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7D));
            if (this.noPhysics) {
                this.moveTowardsClosestSpace(this.getX(),
                        (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.getZ());
            }
        }

        if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > (double) 1.0E-5F
                || (this.tickCount + this.getId()) % 4 == 0) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            float friction = 0.98F;
            if (this.onGround()) {
                BlockPos groundPos = this.getOnPos(0.999999F);
                friction = this.level().getBlockState(groundPos).getFriction(this.level(), groundPos, this) * 0.98F;
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply((double) friction, 0.98D, (double) friction));
            if (this.onGround()) {
                Vec3 motion = this.getDeltaMovement();
                if (motion.y < 0.0D) {
                    this.setDeltaMovement(motion.multiply(1.0D, -0.5D, 1.0D));
                }
            }
        }

        boolean moved = Mth.floor(this.xo) != Mth.floor(this.getX())
                || Mth.floor(this.yo) != Mth.floor(this.getY())
                || Mth.floor(this.zo) != Mth.floor(this.getZ());
        int mergeInterval = moved ? 2 : 40;
        if (this.tickCount % mergeInterval == 0 && !this.level().isClientSide && this.isMergable()) {
            this.mergeWithNeighbours();
        }

        if (this.age != -32768) {
            ++this.age;
        }

        this.hasImpulse |= this.updateInWaterStateAndDoFluidPushing();
        if (!this.level().isClientSide) {
            double impulse = this.getDeltaMovement().subtract(vec3).lengthSqr();
            if (impulse > 0.01D) {
                this.hasImpulse = true;
            }
        }

        if (!this.level().isClientSide && this.age >= this.lifespan) {
            this.discard();
        }

        if (this.getItem().isEmpty() && !this.isRemoved()) {
            this.discard();
        }
    }

    private void setUnderwaterMovement() {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * (double) 0.99F,
                vec3.y + (double) (vec3.y < (double) 0.06F ? 5.0E-4F : 0.0F),
                vec3.z * (double) 0.99F);
    }

    private void setUnderLavaMovement() {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * (double) 0.95F,
                vec3.y + (double) (vec3.y < (double) 0.06F ? 5.0E-4F : 0.0F),
                vec3.z * (double) 0.95F);
    }

    private boolean isMergable() {
        ItemStack stack = this.getItem();
        return this.isAlive() && !stack.isEmpty() && stack.getCount() < stack.getMaxStackSize();
    }

    private void mergeWithNeighbours() {
        if (!this.isMergable()) {
            return;
        }
        List<AtomItemEntity> neighbours = this.level().getEntitiesOfClass(AtomItemEntity.class,
                this.getBoundingBox().inflate(MERGE_RANGE, 0.0D, MERGE_RANGE),
                other -> other != this && other.isMergable());
        for (AtomItemEntity other : neighbours) {
            if (this.isRemoved()) {
                break;
            }
            this.tryToMerge(other);
        }
    }

    private void tryToMerge(AtomItemEntity other) {
        ItemStack mine = this.getItem();
        ItemStack theirs = other.getItem();
        if (mine.isEmpty() || theirs.isEmpty()) {
            return;
        }
        if (!ItemStack.isSameItemSameTags(mine, theirs)) {
            return;
        }
        int max = Math.min(mine.getMaxStackSize(), theirs.getMaxStackSize());
        if (mine.getCount() + theirs.getCount() > max) {
            return;
        }
        this.setItem(mine.copyWithCount(mine.getCount() + theirs.getCount()));
        this.age = Math.min(this.age, other.getAge());
        other.discard();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("Lifespan", this.lifespan);
        if (!this.getItem().isEmpty()) {
            tag.put("Item", this.getItem().save(new CompoundTag()));
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.age = tag.getInt("Age");
        if (tag.contains("Lifespan")) {
            this.lifespan = tag.getInt("Lifespan");
        }
        if (tag.contains("Item")) {
            this.setItem(ItemStack.of(tag.getCompound("Item")));
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public @NotNull Component getName() {
        return Component.empty().append(this.getItem().getItem().getName(this.getItem())).append(" *").append(String.valueOf(this.getItem().getCount()));
    }
}
