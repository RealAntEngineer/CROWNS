package com.rae.crowns.content.nuclear;

import com.rae.crowns.CROWNS;
import com.rae.crowns.api.nuclear.IAmFissileMaterial;
import com.rae.crowns.api.nuclear.IAmRadioactiveSource;
import com.rae.crowns.api.nuclear.IHaveTemperature;
import com.rae.crowns.config.CROWNSConfigs;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rae.crowns.api.Constants.barnNa;
import static com.rae.crowns.api.Constants.fissionEnergy;
import static java.lang.Float.NaN;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IAmRadioactiveSource, IAmFissileMaterial, IHaveGoggleInformation {

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }
    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    public float temperature = 300;
    public float backgroundActivity = 12*3;//In MBq ( giga becquerels ) uranium is 12 Mbq per tonnes
    public float nbrOfFission;//nbr of fission/t
    public float C = 3000*200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly

    public float additionalNeutronsAbsorbed = 0;
    public HashMap<ResourceLocation,Float> radioactiveElements = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"),0.014f*0.2f,
                    CROWNS.resource("u238"),0.986f*0.2f,
                    CROWNS.resource("p239"),0.00f*0.2f
                    ));//for U235,U358 and Plutonium -> percentage of total mass

    //calculate from cross-section (barn), depth (1 meter) and concentration ( as mox fuel isn't a 1m by 1m block of uranium)
    // Absorption law :
    // I = I0* exp(-ln ( dx * c * PI/4 +1 )/dx*L)
    // ( dx = 2*sqrt(sigma/pi) the diameter of a circle of cross-section sigma, c the concentration in mol.m-3 and L the length in m)

    //According to the wikipedia page : https://en.wikipedia.org/wiki/Neutron_cross_section
    // the correct formula is r = N * Flux * sigma
    // this should work only if r N is small ( here we are considering a huge volume of 1 cubic meter )
    // 800 moles of uranium for pure metal *  the mass fraction define in radioactive elements ( fraction of the total mass of the assembly )


    public static HashMap<ResourceLocation,Couple<Float>> fissileCrossSection = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"),Couple.create(1f,583f), //cross-section in barn
                    CROWNS.resource("u238"),Couple.create(0.3f,0.0001f),
                    CROWNS.resource("p239"),Couple.create(2f,748f)

            ));//for U235,U358 and Plutonium -> percentage of total mass
    public static HashMap<ResourceLocation,Float> molarConcentration = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"),19/235f*10000, //amount of moles in a cubic meter of pure metal
                    CROWNS.resource("u238"),19/238f*10000,
                    CROWNS.resource("p239"),19/239f*10000

            ));//for U235,U358 and Plutonium -> percentage of total mass

    public AssemblyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState state) {
        super(blockEntityType, blockPos, state);
        nbrOfFission = backgroundActivity;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }
    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide()) {
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }
        }
        if (Float.isNaN(temperature)){
            temperature = 300;
        }
    }

    @Override
    public void lazyTick(){
        if (!level.isClientSide()) {
            nbrOfFission = additionalNeutronsAbsorbed+backgroundActivity; //for now a 100% change of fission
            if (Float.isNaN(nbrOfFission)){
                nbrOfFission = backgroundActivity;
            }
            additionalNeutronsAbsorbed = 0;
            BlockPos pos = getBlockPos();

            //float thermal_loses = (temperature-300)*10;// ambient temperature = 300K make thermal loses in the conduct temperature

            float power = (float) (nbrOfFission*fissionEnergy *
                    CROWNSConfigs.SERVER.constants.realismCoefficient.get());// - thermal_loses;

            temperature += power/C;

            if (temperature > 3000) {
                if (power > 1000000) {
                    explosion(pos);
                } else {
                    meltdown(pos);
                }
            }
            else {
                if (nbrOfFission < 10 * backgroundActivity) {
                    level.setBlock(pos, getBlockState().setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.NONE), 3);
                } else if (nbrOfFission < 100 * backgroundActivity) {
                    level.setBlock(pos, getBlockState().setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), 3);
                } else {
                    level.setBlock(pos, getBlockState().setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.HIGH), 3);

                }
            }
            moreOptimizedImpactEnv(pos,level,CROWNSConfigs.SERVER.constants.assemblyRange.get());
            conductTemperature(pos,level);
            notifyUpdate();
        }
    }

    /*private void initialisePosGraph() {
        BlockPos pos = getBlockPos();
        // parent/children
        posGraph = new ArrayList<>();
        posGraph.add(new HashMap<>());
        posGraph.get(0).put(pos, List.of(pos.above(),pos.below(),pos.north(),pos.south(),pos.east(),pos.west()));

        ArrayList<BlockPos> listOfComputedParents = new ArrayList<>();
        listOfComputedParents.add(pos);

        for (int i = 1; i < range; i++) {

            HashMap<BlockPos,List<BlockPos>> currentMap = new HashMap<>();
            HashMap<BlockPos,List<BlockPos>> prevMap = posGraph.get(i-1);
            ArrayList<BlockPos> parents = new ArrayList<>();
            //prevent doubles + unordered for performance -> check utility + if the doubles are needed
            for (List<BlockPos> prevChildren: prevMap.values().stream().unordered().distinct().toList()) {
                parents.addAll(prevChildren);
            }
            listOfComputedParents.addAll(parents);
            ArrayList<BlockPos> children = new ArrayList<>();
            for (BlockPos parentPos :
                    parents) {
                for (Direction dir:
                        Direction.values()) {
                    if (!listOfComputedParents.contains(parentPos.relative(dir))) {
                        children.add(parentPos.relative(dir));
                    }
                }
                currentMap.put(parentPos,children);
            }
            posGraph.add(currentMap);
        }
    }*/

    private void meltdown(BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        //level.removeBlockEntity(pos);
    }

    private void explosion(BlockPos pos) {
        float power = 50f;
        assert level != null;
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, true, Explosion.BlockInteraction.BREAK);

        // Remove the block after the explosion
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public float getThermalCapacity() {
        return C;
    }
    //transmition coef
    @Override
    public float getThermalConductivity() {
        return 10000;
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public void addTemperature(float dT) {
        temperature += dT;
    }

    @Override
    public float getRadioactiveActivity() {
        float easeCoef = 1f;//TODO config
        return backgroundActivity+nbrOfFission * 2.5f*easeCoef;
    }

    //to optimise, cost too much on the server

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        tag.putFloat("nbrOfFission", nbrOfFission);
        tag.putFloat("additionalNeutrons",additionalNeutronsAbsorbed);
        tag.putFloat("temperature",temperature);

    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {

        nbrOfFission = tag.getFloat("nbrOfFission");
        additionalNeutronsAbsorbed = tag.getFloat("additionalNeutrons");
        temperature = tag.getFloat("temperature");
        super.read(tag, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        Lang.builder().add(Component.literal("activity : "+ (int)nbrOfFission*20))
                .add(Component.literal(" MBq"))
                .style(ChatFormatting.DARK_GREEN)
                .forGoggles(tooltip, 1);

        Lang.builder().add(Component.literal("temperature : "+(int)temperature))
                .add(Component.literal("°K"))
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);

        return true;
    }

    @Override
    public Couple<Float> absorbNeutrons(Couple<Float> radiationFlux) {
        Float temperatureCoef = 1/Math.max(1,(temperature-300)/600);
        Float fastAbsorbed = 0f;
        Float slowAbsorbed = 0f;
        for (ResourceLocation resourceLocation: radioactiveElements.keySet()) {
            Float massFrac  = radioactiveElements.get(resourceLocation);
            Float cm = AssemblyBlockEntity.molarConcentration.get(resourceLocation);
            Float fastAbsorptionChance = Math.min(1,
                    AssemblyBlockEntity.fissileCrossSection.get(resourceLocation).getFirst()
                            *massFrac*cm*barnNa);
            Float slowAbsorptionChance = Math.min(1,
                    AssemblyBlockEntity.fissileCrossSection.get(resourceLocation).getSecond()
                            *massFrac*cm*barnNa);
            //System.out.println(resourceLocation);
            //System.out.println("fastC : "+ fastAbsorptionChance);
            //System.out.println("slowC : "+ slowAbsorptionChance);
            fastAbsorbed += radiationFlux.getFirst() * temperatureCoef * fastAbsorptionChance;
            slowAbsorbed += radiationFlux.getSecond() * temperatureCoef * slowAbsorptionChance;
        }
        additionalNeutronsAbsorbed += fastAbsorbed + slowAbsorbed;
        return Couple.create(radiationFlux.getFirst()-fastAbsorbed,radiationFlux.getSecond()-slowAbsorbed);
    }
}
