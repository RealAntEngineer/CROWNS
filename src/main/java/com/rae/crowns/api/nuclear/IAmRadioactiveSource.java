package com.rae.crowns.api.nuclear;

import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public interface IAmRadioactiveSource {


    /**
     * @return an amount of neutron/tick
     */
    float getRadioactiveActivity();

    //@Nonnull
    //List<HashMap<BlockPos, List<BlockPos>>> getPosGraph();

    /**
     * make radiation impact the environment
     * @param range :  the range of impact
     */
    /*default void impactEnvironment(BlockPos pos,Level level,int range) {
        assert level!=null;

        Float fastNeutrons = getRadioactiveActivity();
        Float slowNeutrons = 0f;

        HashMap<BlockPos, Couple<Float>> mapOfPosTransmittedNeutrons = new HashMap<>();
        mapOfPosTransmittedNeutrons.put(pos,Couple.create(fastNeutrons,slowNeutrons));

        List<HashMap<BlockPos, List<BlockPos>>> posGraph = getPosGraph();

        // generating the map of every block in range
        //generate at the start

        //calculating the amount of collisions
        Couple<Float> fluxTransmitted;
        for (int i = 0; i < range; i++) {
            HashMap<BlockPos, List<BlockPos>> currentMap = posGraph.get(i);
            for (BlockPos parent:
                    currentMap.keySet()) {

                Couple<Float> radiationFlux = mapOfPosTransmittedNeutrons.get(parent);//should be divided by the nbr of child
                radiationFlux = Couple.create(radiationFlux.getFirst()/((float)currentMap.get(parent).size()),radiationFlux.getSecond()/((float)currentMap.get(parent).size()));
                fluxTransmitted = radiationFlux;//transmit every thing by default
                for (BlockPos child: currentMap.get(parent)){

                    BlockEntity childBE = level.getBlockEntity(child);
                    if (childBE instanceof IAmFissileMaterial fissileMaterial){
                        fluxTransmitted = fissileMaterial.absorbNeutrons(radiationFlux);

                    }
                    BlockState state = level.getBlockState(child);

                    if (state.is(new TagKey<>(ForgeRegistries.BLOCKS.getRegistryKey(),new ResourceLocation("forge:storage_blocks/coal")))){
                        fluxTransmitted = Couple.create(radiationFlux.getFirst()*(1- 0.7f), radiationFlux.getSecond()+radiationFlux.getFirst()* (Float) 0.7f);
                    }
                    if (state.is(new TagKey<>(ForgeRegistries.BLOCKS.getRegistryKey(),new ResourceLocation("forge:storage_blocks/gold")))){
                        fluxTransmitted = Couple.create(0f,0f)//Couple.create(radiationFlux.getFirst()*0.5f, radiationFlux.getSecond()+radiationFlux.getFirst()*0.5f);
                        ;
                    }
                    FluidState fluidState = level.getFluidState(child);
                    if (!fluidState.isEmpty()){
                        if (fluidState.is(FluidTags.WATER)){

                            fluxTransmitted = Couple.create(radiationFlux.getFirst()*(1- 0.5f), radiationFlux.getSecond()+radiationFlux.getFirst()* (Float) 0.5f);
                        }
                    }
                    mapOfPosTransmittedNeutrons.put(child,fluxTransmitted);
                }
            }
        }
    }*/
    //20 000 times * traceNeutron for a range of 3. It's way too many
    //new idea : get a list of every block of the frontier of a circle of this size then iterate on it

    /**
     * make radiation impact the environment
     * @param pos : the center of a block
     * @param level : a server level
     * @param range :  the range of impact
     */
    default void impactEnvironmentBetter(BlockPos pos, Level level, Double range){

        //there is some duplicate -> to optimise
        Float fastNeutrons = getRadioactiveActivity();
        Float slowNeutrons = 0f;
        //should impact itself
        for (float phi = (float) (-Math.PI/2f); phi <=Math.PI/2f; phi+= (1/ (float) (2*Math.PI*range))){
            float y = (float) (Math.sin(phi)*range);
            float radius = (float) (Math.cos(phi)*range);

            if (radius <1f) {
                Vec3 vec = new Vec3(0, y/range, 0);
                //testRadio(pos, level, range, vec);
                traceNeutron(pos, level, range, vec, fastNeutrons);
            }
            else {
                for (float theta = 0f; theta <= 2 * Math.PI; theta += (1/ (float) (2*Math.PI*radius))) {
                    Vec3 vec = new Vec3(Math.cos(theta) * radius/range, y/range, Math.sin(theta) * radius/range);
                    //testRadio(pos, level, range, vec);
                    traceNeutron(pos, level, range, vec, fastNeutrons);
                }
            }
        }
    }
    private static void testRadio(BlockPos pos, Level level, int range, Vec3 vec) {
            Vec3i partialVec = new Vec3i(vec.x() * range + 0.5f, vec.y() * range + 0.5f, vec.z() * range + 0.5f);
            BlockPos child = pos.offset(partialVec);
            level.setBlock(child, Blocks.STONE.defaultBlockState(), 11);
    }
    private static void traceNeutron(BlockPos pos, Level level, Double range, Vec3 vec, Float fastNeutrons) {
        Vec3 newVec = vec.scale((double) 1 / range);
        //the surface isn't really a constant so a bit wrong
        //TODO make the surface a variable
        Couple<Float> radiationFlux = Couple.create((float) (50*fastNeutrons /(4*Math.PI* range * range)),0f);
        for (int i = 1; i <= range; i++) {
            Vec3i partialVec = new Vec3i(newVec.x()* i+0.5f, newVec.y()* i+0.5f, newVec.z()* i+0.5f);
            BlockPos child = pos.offset(partialVec);
            BlockEntity childBE = level.getBlockEntity(child);
            if (childBE instanceof IAmFissileMaterial fissileMaterial){
                radiationFlux = fissileMaterial.absorbNeutrons(radiationFlux);

            }
            BlockState state = level.getBlockState(child);

            if (state.is(new TagKey<>(ForgeRegistries.BLOCKS.getRegistryKey(),new ResourceLocation("forge:storage_blocks/coal")))){
                radiationFlux = Couple.create(radiationFlux.getFirst()*(1- 0.7f), radiationFlux.getSecond()+ radiationFlux.getFirst()* (Float) 0.7f);
            }
            if (state.is(new TagKey<>(ForgeRegistries.BLOCKS.getRegistryKey(),new ResourceLocation("forge:storage_blocks/gold")))){
                radiationFlux = Couple.create(0f,0f)//Couple.create(radiationFlux.getFirst()*0.5f, radiationFlux.getSecond()*0.5f);
                ;
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
