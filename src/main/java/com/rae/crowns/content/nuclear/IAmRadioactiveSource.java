package com.rae.crowns.content.nuclear;

import com.rae.crowns.content.RayTraceUtil;
import com.rae.crowns.content.nuclear.rod.IRodContainerBlockEntity;
import com.rae.crowns.content.nuclear.rod.RodBlock;
import com.rae.crowns.init.misc.TagsInit;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;

import java.util.List;

@NonnullDefault
public interface IAmRadioactiveSource {


    double BETA            = 0.0065;         // effective delayed neutron fraction
    double LAMBDA          = 0.08;         // decay constant of delayed neutron precursors (1/s)
    double PROMPT_LIFETIME = 2e-5; // prompt neutron lifetime (s)

    /**
     * Calculates reactivity (in Δk/k) from two fission count values.
     *
     * @param oldFissionCount previous fission count (proportional to n(t))
     * @param newFissionCount current fission count (proportional to n(t+dt))
     * @param deltaTime       time between the two measurements (in seconds)
     * @return reactivity in Δk/k
     */
    static double computeReactivity(double oldFissionCount, double newFissionCount, double deltaTime) {
        if (oldFissionCount <= 0 || deltaTime <= 0) {
            throw new IllegalArgumentException("Fission count and deltaTime must be positive.");
        }

        // Normalize population
        double n  = newFissionCount;
        double dn = (newFissionCount - oldFissionCount) / deltaTime;

        // Inverse kinetics (1 delayed neutron group)
        double reactivity = PROMPT_LIFETIME * (dn / n) +
                BETA * (1 - 1 / (1 + (1.0 / LAMBDA) * (dn / n)));

        return reactivity;
    }

    @Deprecated
    default int impactEnv(BlockPos pos, Level level, Double range) {
        float fastNeutrons = getRadioactiveActivity();
        float slowNeutrons = 0f;
        int   rays         = 0;
        //should impact itself
        List<BlockPos> frontier     = RayTraceUtil.getSphereSurface(BlockPos.ZERO, range.intValue(), true);
        double         rangeInverse = 1 / range;
        for (BlockPos frontierPos : frontier) {

            Vec3 vec = new Vec3(frontierPos.getX(), frontierPos.getY(), frontierPos.getZ()).scale(rangeInverse);
            traceNeutron(pos, level, range, vec, fastNeutrons);
            rays++;

        }
        return rays;
    }

    /**
     * @return an amount of neutron/tick
     */
    float getRadioactiveActivity();

    /**
     * make radiation impact the environment
     *
     * @param pos   : the center of a block
     * @param level : a server level
     * @param range :  the range of impact
     */
    @Deprecated
    private static void traceNeutron(BlockPos pos, Level level, double range, Vec3 vec, float fastNeutrons) {
        //the surface isn't really a constant so a bit wrong
        //TODO make the surface a variable + why 50 ?
        //Couple<Float> radiationFlux = Couple.create((float) (50 * fastNeutrons / (4 * Math.PI * range * range)), 0f);
        double dx = vec.x;
        double dy = vec.y;
        double dz = vec.z;

        double cx = pos.getX();
        double cy = pos.getY();
        double cz = pos.getZ();

        float fast    = (float) (50 * fastNeutrons / (4 * Math.PI * range * range));
        float thermal = 0f;

        BlockPos.MutableBlockPos child = new BlockPos.MutableBlockPos();
        for (int i = 1; i <= range; i++) {
            cx += dx;
            cy += dy;
            cz += dz;

            child.set((int) cx, (int) cy, (int) cz);

            BlockState state = level.getBlockState(child);
            if (state.hasBlockEntity()) {
                BlockEntity childBE = level.getBlockEntity(child);
                if (childBE instanceof IAmFissileMaterial fissileMaterial) {
                    Couple<Float> result = fissileMaterial.absorbNeutrons(Couple.create(fast, thermal));
                    fast = result.getFirst();
                    thermal = result.getSecond();

                }
            }
            if (!state.isAir()) {
                if (TagsInit.CustomBlockTags.COAL_BLOCK.matches(state)) {
                    float slowed = fast * 0.7f;
                    fast -= slowed;
                    thermal += slowed;
                    //radiationFlux = Couple.create(radiationFlux.getFirst() * (1 - 0.7f), radiationFlux.getSecond() + radiationFlux.getFirst() * 0.7f);
                } else if (TagsInit.CustomBlockTags.GOLD_BLOCK.matches(state)) {
                    //radiationFlux = Couple.create(0f,0f);//Couple.create(radiationFlux.getFirst()*0.5f, radiationFlux.getSecond()*0.5f);
                    break;
                }
            }

            FluidState fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
                if (fluidState.is(FluidTags.WATER)) {
                    float absorbed = fast * 0.5f;
                    fast -= absorbed;
                    thermal += absorbed;
                    //radiationFlux = Couple.create(radiationFlux.getFirst() * (1 - 0.5f), radiationFlux.getSecond() + radiationFlux.getFirst() * (Float) 0.5f);
                }
            }
        }
    }

    //inline ray tracing for radiation
    default int moreOptimizedImpactEnv(BlockPos pos, Level level, Double range) {

        float fastNeutrons = getRadioactiveActivity();
        int   rays         = 0;

        int    steps        = range.intValue();
        double rangeInverse = 1 / range;

        List<BlockPos> frontier = RayTraceUtil.getSphereSurface(BlockPos.ZERO, steps, true);

        // ---- caches ----
        Long2ObjectOpenHashMap<BlockEntity> beCache =
                new Long2ObjectOpenHashMap<>();

        Object2BooleanOpenHashMap<Block> fissileBlockCache =
                new Object2BooleanOpenHashMap<>();

        BlockPos.MutableBlockPos child = new BlockPos.MutableBlockPos();

        for (BlockPos frontierPos : frontier) {

            double dx = frontierPos.getX() * rangeInverse;
            double dy = frontierPos.getY() * rangeInverse;
            double dz = frontierPos.getZ() * rangeInverse;

            double cx = pos.getX();
            double cy = pos.getY();
            double cz = pos.getZ();

            float fast    = (float) (50 * fastNeutrons / (4 * Math.PI * range * range));
            float thermal = 0f;

            for (int i = 0; i < steps; i++) {

                cx += dx;
                cy += dy;
                cz += dz;

                int bx = (int) cx;
                int by = (int) cy;
                int bz = (int) cz;

                child.set(bx, by, bz);

                BlockState state = level.getBlockState(child);
                Block      block = state.getBlock();

                // ---------- fissile BE logic ----------
                boolean hasBlockEntity = fissileBlockCache.computeIfAbsent(
                        block,
                        b -> state.hasBlockEntity() // fast prefilter
                );

                BlockEntity be = null;
                if (hasBlockEntity) {
                    be = cachedBlockEntity(level, child, beCache);

                    if (be instanceof IAmFissileMaterial fissile) {
                        Couple<Float> result =
                                fissile.absorbNeutrons(Couple.create(fast, thermal));
                        fast = result.getFirst();
                        thermal = result.getSecond();
                    }
                }

                // ---------- rod moderation / absorption ----------
                // Check self first (reuses the lookup above instead of another lookup).
                // Only fall back to top/bottom if self isn't a rod container: a rod
                // can hang past the end of its placed column into a cell with no
                // block entity of its own, so the nearest container holding that rod's
                // state may actually be one cell above or below.
                IRodContainerBlockEntity rod        = be instanceof IRodContainerBlockEntity r ? r : null;
                float                    moderation = 0;
                float                    absorption = 0;
                if (rod == null) {
                    BlockEntity aboveBE = cachedBlockEntity(level, child.above(), beCache);
                    if (aboveBE instanceof IRodContainerBlockEntity r && r.getRodContained() instanceof RodBlock insertedRod) {
                        float occupiedInserted = Math.abs(r.getOffset());

                        moderation += occupiedInserted * insertedRod.getMaterialModeration();
                        absorption += occupiedInserted * insertedRod.getMaterialAbsorption();
                    }
                    BlockEntity belowBE = cachedBlockEntity(level, child.below(), beCache);
                    if (belowBE instanceof IRodContainerBlockEntity r && r.getRodContained() instanceof RodBlock insertedRod) {
                        float occupiedInserted = Math.abs(r.getOffset());

                        moderation += occupiedInserted * insertedRod.getMaterialModeration();
                        absorption += occupiedInserted * insertedRod.getMaterialAbsorption();
                    }

                } else {
                    moderation = rod.getModeration();
                    absorption = rod.getAbsorption();

                }

                if (moderation > 0f) {
                    float slowed = fast * moderation;
                    fast -= slowed;
                    thermal += slowed;
                }

                if (absorption > 0f) {
                    fast -= fast * absorption;
                    thermal -= thermal * absorption;
                }


                // ---------- coal ----------
                if (!state.isAir()) {
                    if (TagsInit.CustomBlockTags.COAL_BLOCK.matches(state)) {
                        float slowed = fast * 0.7f;
                        fast -= slowed;
                        thermal += slowed;
                    }

                    // ---------- gold ----------
                    else if (TagsInit.CustomBlockTags.GOLD_BLOCK.matches(state)) {
                        break;
                    }
                }

                // ---------- water ----------
                FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty() && fluid.is(FluidTags.WATER)) {
                    float absorbed = fast * 0.5f;
                    fast -= absorbed;
                    thermal += absorbed;
                }
            }

            rays++;
        }

        return rays;
    }

    private static @Nullable BlockEntity cachedBlockEntity(Level level, BlockPos pos,
                                                           Long2ObjectOpenHashMap<BlockEntity> cache) {
        long        key = pos.asLong();
        BlockEntity be  = cache.get(key);
        if (be == null && !cache.containsKey(key)) {
            be = level.getBlockEntity(pos);
            cache.put(key, be);
        }
        return be;
    }
}
