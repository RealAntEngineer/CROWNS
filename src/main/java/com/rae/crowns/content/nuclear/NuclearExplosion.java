package com.rae.crowns.content.nuclear;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.rae.crowns.content.RayTraceUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NuclearExplosion extends Explosion {
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
    private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
    private final boolean fire;
    private final Explosion.@NotNull BlockInteraction blockInteraction;
    private final @NotNull RandomSource random;
    private final @NotNull Level level;
    private final double x;
    private final double y;
    private final double z;
    @javax.annotation.Nullable
    private final Entity source;
    private final float radius;
    private final @NotNull DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final @NotNull ObjectArrayList<BlockPos> toBlow;
    private final @NotNull Map<Player, Vec3> hitPlayers;

    public NuclearExplosion(@NotNull Level level, @Nullable Entity source, double x, double y, double z, float radius, List<BlockPos> toBlow, Explosion.@NotNull BlockInteraction blockInteraction) {
        this(level, source, getDefaultDamageSource(level, source), null, x, y, z, radius, false, blockInteraction);
        this.toBlow.addAll(toBlow);
    }

    public NuclearExplosion(@NotNull Level level, @Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float radius, boolean fire, Explosion.@NotNull BlockInteraction blockInteraction) {
        super(level, source, damageSource, damageCalculator, x, y, z, radius, fire, blockInteraction);
        this.random = RandomSource.create();
        this.toBlow = new ObjectArrayList<>();
        this.hitPlayers = Maps.newHashMap();
        this.level = level;
        this.source = source;
        this.radius = radius;
        this.x = x;
        this.y = y;
        this.z = z;
        this.fire = fire;
        this.blockInteraction = blockInteraction;
        this.damageSource = damageSource == null ? level.damageSources().explosion(this.getDirectSourceEntity(), this.getIndirectSourceEntity()) : damageSource;
        this.damageCalculator = damageCalculator == null ? this.makeDamageCalculator(source) : damageCalculator;
    }

    public static @NotNull DamageSource getDefaultDamageSource(@NotNull Level level, @Nullable Entity source) {
        return level.damageSources().explosion(source, getIndirectSourceEntityInternal(source));
    }

    private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity entity) {
        return entity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(entity);
    }

    @Nullable
    private static LivingEntity getIndirectSourceEntityInternal(@Nullable Entity source) {
        if (source == null) {
            return null;
        } else {
            Entity entity = source;
            if (entity instanceof PrimedTnt primedtnt) {
                return primedtnt.getOwner();
            } else {
                entity = source;
                if (entity instanceof LivingEntity livingentity) {
                    return livingentity;
                } else {
                    entity = source;
                    if (entity instanceof Projectile projectile) {
                        entity = projectile.getOwner();
                        if (entity instanceof LivingEntity) {
                            return (LivingEntity) entity;
                        }
                    }

                    return null;
                }
            }
        }
    }

    public NuclearExplosion(@NotNull Level level, @Nullable Entity source, double x, double y, double z, float radius, boolean fire, Explosion.@NotNull BlockInteraction blockInteraction, List<BlockPos> positions) {
        this(level, source, x, y, z, radius, fire, blockInteraction);
        this.toBlow.addAll(positions);
    }

    public NuclearExplosion(@NotNull Level level, @Nullable Entity source, double x, double y, double z, float radius, boolean fire, Explosion.@NotNull BlockInteraction blockInteraction) {
        this(level, source, getDefaultDamageSource(level, source), null, x, y, z, radius, fire, blockInteraction);
    }

    public static void nuclearExplosion(@NotNull Level level, @NotNull BlockPos pos, float power) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);

        NuclearExplosion explosion = new NuclearExplosion(level, null, null,
                null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, false,
                level.getGameRules().getBoolean(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY);
        if (!ForgeEventFactory.onExplosionStart(level, explosion)) {
            explosion.explode();
            explosion.finalizeExplosion(true);
        }
    }

    public void explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
        List<Vec3> surface = RayTraceUtil.getSphereSurface(this.center(), this.radius());//just the exterior
        Set<BlockPos> set = Sets.newHashSet();

        for (Vec3 pos : surface) {//pos : maximal position on the direction (absolute)

            float f = this.radius * (0.2F);//+ this.level.random.nextFloat() * 0.3F);//explosion strengh on the direction

            for (int i = 0; i < radius; i++) {
                BlockPos blockpos = BlockPos.containing(center().add(pos.subtract(center()).scale(i / radius)));//linear interpolation
                BlockState blockstate = this.level.getBlockState(blockpos);
                FluidState fluidstate = this.level.getFluidState(blockpos);
                if (!this.level.isInWorldBounds(blockpos)) {
                    break;
                }

                Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
                if (optional.isPresent()) {
                    f -= (optional.get() + 0.3F) * 0.3F;
                }

                if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) {
                    set.add(blockpos);
                } else {
                    break;
                }
                f -= 0.2F;
            }

        }

        this.toBlow.addAll(set);
        float f2 = this.radius * 2.0F;
        int k1 = Mth.floor(this.x - (double) f2 - (double) 1.0F);
        int l1 = Mth.floor(this.x + (double) f2 + (double) 1.0F);
        int i2 = Mth.floor(this.y - (double) f2 - (double) 1.0F);
        int i1 = Mth.floor(this.y + (double) f2 + (double) 1.0F);
        int j2 = Mth.floor(this.z - (double) f2 - (double) 1.0F);
        int j1 = Mth.floor(this.z + (double) f2 + (double) 1.0F);
        List<Entity> list = this.level.getEntities(this.source, new AABB(k1, i2, j2, l1, i1, j1));
        ForgeEventFactory.onExplosionDetonate(this.level, this, list, f2);
        Vec3 vec3 = new Vec3(this.x, this.y, this.z);

        for (Entity entity : list) {
            if (!entity.ignoreExplosion()) {
                double d12 = Math.sqrt(entity.distanceToSqr(vec3)) / (double) f2;
                if (d12 <= 1.0D) {
                    double d5 = entity.getX() - this.x;
                    double d7 = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                    double d9 = entity.getZ() - this.z;
                    double d13 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                    if (d13 != 0.0D) {
                        d5 /= d13;
                        d7 /= d13;
                        d9 /= d13;
                        double d14 = getSeenPercent(vec3, entity);
                        double d10 = (1.0D - d12) * d14;
                        entity.hurt(this.getDamageSource(), (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f2 + 1.0D)));
                        double d11;
                        if (entity instanceof LivingEntity livingentity) {
                            d11 = ProtectionEnchantment.getExplosionKnockbackAfterDampener(livingentity, d10);
                        } else {
                            d11 = d10;
                        }

                        d5 *= d11;
                        d7 *= d11;
                        d9 *= d11;
                        Vec3 vec31 = new Vec3(d5, d7, d9);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(vec31));
                        if (entity instanceof Player player) {
                            if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                                this.hitPlayers.put(player, vec31);
                            }
                        }
                    }
                }
            }
        }

    }

    public @NotNull Vec3 center() {
        return new Vec3(this.x, this.y, this.z);
    }

    public float radius() {
        return this.radius;
    }

    public static float getSeenPercent(@NotNull Vec3 explosionVector, @NotNull Entity entity) {
        AABB aabb = entity.getBoundingBox();
        double d0 = (double) 1.0F / ((aabb.maxX - aabb.minX) * (double) 2.0F + (double) 1.0F);
        double d1 = (double) 1.0F / ((aabb.maxY - aabb.minY) * (double) 2.0F + (double) 1.0F);
        double d2 = (double) 1.0F / ((aabb.maxZ - aabb.minZ) * (double) 2.0F + (double) 1.0F);
        double d3 = ((double) 1.0F - Math.floor((double) 1.0F / d0) * d0) / (double) 2.0F;
        double d4 = ((double) 1.0F - Math.floor((double) 1.0F / d2) * d2) / (double) 2.0F;
        if (!(d0 < (double) 0.0F) && !(d1 < (double) 0.0F) && !(d2 < (double) 0.0F)) {
            int i = 0;
            int j = 0;

            for (double d5 = 0.0F; d5 <= (double) 1.0F; d5 += d0) {
                for (double d6 = 0.0F; d6 <= (double) 1.0F; d6 += d1) {
                    for (double d7 = 0.0F; d7 <= (double) 1.0F; d7 += d2) {
                        double d8 = Mth.lerp(d5, aabb.minX, aabb.maxX);
                        double d9 = Mth.lerp(d6, aabb.minY, aabb.maxY);
                        double d10 = Mth.lerp(d7, aabb.minZ, aabb.maxZ);
                        Vec3 vec3 = new Vec3(d8 + d3, d9, d10 + d4);
                        if (entity.level().clip(new ClipContext(vec3, explosionVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS) {
                            ++i;
                        }

                        ++j;
                    }
                }
            }

            return (float) i / (float) j;
        } else {
            return 0.0F;
        }
    }

    private static void addBlockDrops(@NotNull ObjectArrayList<Pair<ItemStack, BlockPos>> p_46068_, @NotNull ItemStack p_46069_, BlockPos p_46070_) {
        int i = p_46068_.size();

        for (int j = 0; j < i; ++j) {
            Pair<ItemStack, BlockPos> pair = p_46068_.get(j);
            ItemStack itemstack = pair.getFirst();
            if (ItemEntity.areMergable(itemstack, p_46069_)) {
                ItemStack itemstack1 = ItemEntity.merge(itemstack, p_46069_, 16);
                p_46068_.set(j, Pair.of(itemstack1, pair.getSecond()));
                if (p_46069_.isEmpty()) {
                    return;
                }
            }
        }

        p_46068_.add(Pair.of(p_46069_, p_46070_));
    }

    public void finalizeExplosion(boolean spawnParticles) {
        if (this.level.isClientSide) {
            this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
        }

        boolean flag = this.interactsWithBlocks();
        if (spawnParticles) {
            if (!(this.radius < 2.0F) && flag) {
                this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            } else {
                this.level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            }
        }

        if (flag) {
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList<>();
            boolean flag1 = this.getIndirectSourceEntity() instanceof Player;
            Util.shuffle(this.toBlow, this.level.random);

            for (BlockPos blockpos : this.toBlow) {
                BlockState blockstate = this.level.getBlockState(blockpos);
                Block block = blockstate.getBlock();
                if (!blockstate.isAir()) {
                    BlockPos blockpos1 = blockpos.immutable();
                    this.level.getProfiler().push("explosion_blocks");
                    if (blockstate.canDropFromExplosion(this.level, blockpos, this)) {
                        Level $$9 = this.level;
                        if ($$9 instanceof ServerLevel serverlevel) {
                            BlockEntity blockentity = blockstate.hasBlockEntity() ? this.level.getBlockEntity(blockpos) : null;
                            LootParams.Builder lootparams$builder = (new LootParams.Builder(serverlevel)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity).withOptionalParameter(LootContextParams.THIS_ENTITY, this.source);
                            if (this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY) {
                                lootparams$builder.withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius);
                            }

                            blockstate.spawnAfterBreak(serverlevel, blockpos, ItemStack.EMPTY, flag1);
                            blockstate.getDrops(lootparams$builder).forEach((p_46074_) -> {
                                addBlockDrops(objectarraylist, p_46074_, blockpos1);
                            });
                        }
                    }

                    blockstate.onBlockExploded(this.level, blockpos, this);
                    this.level.getProfiler().pop();
                }
            }

            for (Pair<ItemStack, BlockPos> pair : objectarraylist) {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst());
            }
        }

        if (this.fire) {
            for (BlockPos blockpos2 : this.toBlow) {
                if (this.random.nextInt(3) == 0 && this.level.getBlockState(blockpos2).isAir() && this.level.getBlockState(blockpos2.below()).isSolidRender(this.level, blockpos2.below())) {
                    this.level.setBlockAndUpdate(blockpos2, BaseFireBlock.getState(this.level, blockpos2));
                }
            }
        }

    }

    public boolean interactsWithBlocks() {
        return this.blockInteraction != Explosion.BlockInteraction.KEEP;
    }

    public @NotNull Map<Player, Vec3> getHitPlayers() {
        return this.hitPlayers;
    }

    @Nullable
    public LivingEntity getIndirectSourceEntity() {
        return getIndirectSourceEntityInternal(this.source);
    }

    @Nullable
    public Entity getDirectSourceEntity() {
        return this.source;
    }

    public void clearToBlow() {
        this.toBlow.clear();
    }

    public @NotNull List<BlockPos> getToBlow() {
        return this.toBlow;
    }

    private static void addOrAppendStack(@NotNull List<Pair<ItemStack, BlockPos>> drops, @NotNull ItemStack stack, BlockPos pos) {
        for (int i = 0; i < drops.size(); ++i) {
            Pair<ItemStack, BlockPos> pair = drops.get(i);
            ItemStack itemstack = pair.getFirst();
            if (ItemEntity.areMergable(itemstack, stack)) {
                drops.set(i, Pair.of(ItemEntity.merge(itemstack, stack, 16), pair.getSecond()));
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

        drops.add(Pair.of(stack, pos));
    }

}
