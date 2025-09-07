package com.rae.crowns.content.nuclear;

import com.rae.crowns.init.misc.TagsInit;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public interface IAmRadioactiveSource {


    /**
     * @return an amount of neutron/tick
     */
    float getRadioactiveActivity();

    double BETA = 0.0065;         // effective delayed neutron fraction
    double LAMBDA = 0.08;         // decay constant of delayed neutron precursors (1/s)
    double PROMPT_LIFETIME = 2e-5; // prompt neutron lifetime (s)

    /**
     * Calculates reactivity (in Δk/k) from two fission count values.
     *
     * @param oldFissionCount previous fission count (proportional to n(t))
     * @param newFissionCount current fission count (proportional to n(t+dt))
     * @param deltaTime time between the two measurements (in seconds)
     * @return reactivity in Δk/k
     */
    static double computeReactivity(double oldFissionCount, double newFissionCount, double deltaTime) {
        if (oldFissionCount <= 0 || deltaTime <= 0) {
            throw new IllegalArgumentException("Fission count and deltaTime must be positive.");
        }

        // Normalize population
        double n = newFissionCount;
        double dn = (newFissionCount - oldFissionCount) / deltaTime;

        // Inverse kinetics (1 delayed neutron group)
        double reactivity = PROMPT_LIFETIME * (dn / n) +
                BETA * (1 - 1 / (1 + (1.0 / LAMBDA) * (dn / n)));

        return reactivity;
    }
    /**
     * make radiation impact the environment
     * @param pos : the center of a block
     * @param level : a server level
     * @param range :  the range of impact
     */
    private static void traceNeutron(BlockPos pos, Level level, Double range, Vec3 vec, Float fastNeutrons) {
        Vec3 newVec = vec.scale((double) 1 / range);
        //the surface isn't really a constant so a bit wrong
        //TODO make the surface a variable
        //why 50 time ? and why 0.5 ?
        Couple<Float> radiationFlux = Couple.create((float) (50*fastNeutrons /(4*Math.PI* range * range)),0f);
        for (int i = 1; i <= range; i++) {
            Vec3i partialVec = new Vec3i((int) (newVec.x()* i), (int) (newVec.y()* i), (int) (newVec.z()* i));//
            BlockPos child = pos.offset(partialVec);
            BlockEntity childBE = level.getBlockEntity(child);
            if (childBE instanceof IAmFissileMaterial fissileMaterial){
                radiationFlux = fissileMaterial.absorbNeutrons(radiationFlux);
            }
            BlockState state = level.getBlockState(child);

            if (TagsInit.CustomBlockTags.COAL_BLOCK.matches(state)){
                radiationFlux = Couple.create(radiationFlux.getFirst()*(1- 0.7f), radiationFlux.getSecond()+ radiationFlux.getFirst()* (Float) 0.7f);
                continue;
            }
            if (TagsInit.CustomBlockTags.GOLD_BLOCK.matches(state)){
                //radiationFlux = Couple.create(0f,0f);//Couple.create(radiationFlux.getFirst()*0.5f, radiationFlux.getSecond()*0.5f);
                break;
            }
            FluidState fluidState = level.getFluidState(child);
            if (!fluidState.isEmpty()) {
                if (fluidState.is(FluidTags.WATER)) {

                    radiationFlux = Couple.create(radiationFlux.getFirst() * (1 - 0.5f), radiationFlux.getSecond() + radiationFlux.getFirst() * (Float) 0.5f);
                }
            }
        }
    }

    default void moreOptimizedImpactEnv(BlockPos pos, Level level, Double range){
        Float fastNeutrons = getRadioactiveActivity();
        Float slowNeutrons = 0f;
        //should impact itself
        List<BlockPos> frontier = getSphere(BlockPos.ZERO,range.intValue(),true);
        for (BlockPos frontierPos : frontier){

                Vec3 vec = new Vec3(frontierPos.getX(), frontierPos.getY(), frontierPos.getZ());
                traceNeutron(pos, level, range, vec, fastNeutrons);

        }
    }
    private List<BlockPos> getSphere(BlockPos center, int radius, boolean empty) {
        List<BlockPos> blocks = new ArrayList<>();

        int bx = center.getX();
        int by = center.getY();
        int bz = center.getZ();

        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int y = by - radius; y <= by + radius; y++) {
                for (int z = bz - radius; z <= bz + radius; z++) {
                    double distance = ((bx - x) * (bx - x) + (bz - z) * (bz - z) + (by - y) * (by - y));
                    if (distance < radius * radius && (!empty || distance >= (radius - 1) * (radius - 1))) {
                        blocks.add(new BlockPos( x, y, z));
                    }
                }
            }
        }
        return blocks;
    }
}
