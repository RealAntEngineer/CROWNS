package com.rae.crowns.content.nuclear;

import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.misc.FluidInit;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rae.crowns.Constants.barnNa;
import static com.rae.crowns.Constants.fissionEnergy;
import static com.rae.crowns.content.nuclear.NuclearExplosion.nuclearExplosion;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IAmRadioactiveSource, IAmFissileMaterial, IHaveGoggleInformation {

    private static final int SYNC_RATE = 8;
    public float temperature = 300;
    public float backgroundActivity = 12 * 3;//In MBq ( giga becquerels ) uranium is 12 Mbq per tonnes
    public float oldNbrOfFission;
    public float nbrOfFission;//nbr of fission/t
    public float C = 3000 * 200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly
    public float additionalNeutronsAbsorbed = 0;
    public HashMap<ResourceLocation, Float> radioactiveElements = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"), 0.014f * 0.2f,
                    CROWNS.resource("u238"), 0.986f * 0.2f,
                    CROWNS.resource("p239"), 0.00f * 0.2f
            ));//for U235,U358 and Plutonium -> percentage of total mass
    protected int syncCooldown;
    protected boolean queuedSync;
    float power = 0;


    public AssemblyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState state) {
        super(blockEntityType, blockPos, state);
        nbrOfFission = backgroundActivity;
    }

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

            if (CROWNSConfigs.COMMON.nuclearParticle.get())
                spawnRadiationParticles(level, getBlockPos(), nbrOfFission);
            temperature += power / C * 1 / 20f;
        }
        if (Float.isNaN(temperature)) {
            temperature = 300;
        }
    }

    @Override
    public void lazyTick() {
        if (!level.isClientSide()) {
            oldNbrOfFission = nbrOfFission;
            nbrOfFission = additionalNeutronsAbsorbed + backgroundActivity; //for now a 100% change of fission : no absorption
            if (Float.isNaN(nbrOfFission)) {
                nbrOfFission = backgroundActivity;
            }
            additionalNeutronsAbsorbed = 0;
            BlockPos pos = getBlockPos();

            //float thermal_loses = (temperature-300)*10;// ambient temperature = 300K make thermal loses in the conduct temperature

            power = (float) (nbrOfFission * fissionEnergy *
                    CROWNSConfigs.SERVER.nuclear.realismCoefficient.get());// - thermal_loses;

            //temperature += power/C;


            if (temperature > 3500) {
                if (power > 1e9) {
                    standardExplosion(pos, 10);
                } else {
                    meltdown(pos);
                }
            } else {
                if (nbrOfFission < 300 * backgroundActivity) {
                    level.setBlock(pos, getBlockState().setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.NONE), 3);
                } else if (temperature < 3000) {
                    level.setBlock(pos, getBlockState().setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), 3);
                } else {
                    level.setBlock(pos, getBlockState().setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.HIGH), 3);

                }
            }
            moreOptimizedImpactEnv(pos, level, CROWNSConfigs.SERVER.nuclear.radiationRange.get());

            notifyUpdate();

        }
    }

    public void spawnRadiationParticles(Level level, BlockPos pos, float nbrOfFission) {
        if (!(level instanceof ServerLevel serverLevel)) return; // Only spawn particles on server side

        float nbrOfParticles = (float) (Math.log10(nbrOfFission * 20 / 5000f)) * 3f / 20f;
        int wholeParticles = Mth.floor(nbrOfParticles);
        float fractional = nbrOfParticles - wholeParticles;

        if (level.random.nextFloat() < fractional) {
            wholeParticles += 1; // probabilistically add one extra
        }

        for (int i = 0; i < wholeParticles; i++) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            // Random spherical direction using spherical coordinates
            double theta = level.random.nextDouble() * 2 * Math.PI; // azimuthal angle
            double phi = Math.acos(2 * level.random.nextDouble() - 1); // polar angle

            double speed = 1f; // small random speed
            double dx = speed * Math.sin(phi) * Math.cos(theta);
            double dy = speed * Math.sin(phi) * Math.sin(theta);
            double dz = speed * Math.cos(phi);

            // Use any existing particle type here (e.g., SMOKE)
            serverLevel.sendParticles(new DustParticleOptions(Color.WHITE.asVectorF(), 1), x, y, z, 1, dx, dy, dz, speed);// You can replace ParticleTypes.SMOKE with your custom particle
        }
    }

    private void meltdown(BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
        //level.removeBlockEntity(pos);
    }

    private void standardExplosion(BlockPos pos, float power) {
        assert this.level != null;
        nuclearExplosion(this.level, pos, power);
        // Remove the block after the explosion
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
    }

    @Override
    public float getThermalCapacity() {
        return C;
    }

    //transmition coef
    @Override
    public float getThermalConductivity() {
        return CROWNSConfigs.SERVER.conduction.assemblyBlock.getF();
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public void addTemperature(float dT) {
        temperature = Math.max(temperature + dT, 0);
    }

    @Override
    public float getRadioactiveActivity() {
        float easeCoef = CROWNSConfigs.SERVER.nuclear.easeCoef.getF(); //TODO config
        return backgroundActivity + nbrOfFission * 2.5f * easeCoef;
    }

    @Override
    public float getEffectiveK() {
        float easeCoef = 1f; //TODO config
        return (backgroundActivity + nbrOfFission * 2.5f * easeCoef) / (backgroundActivity + oldNbrOfFission * 2.5f * easeCoef);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        tag.putFloat("nbrOfFission", nbrOfFission);
        tag.putFloat("additionalNeutrons", additionalNeutronsAbsorbed);
        tag.putFloat("temperature", temperature);
        tag.putFloat("power", power);

    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {

        nbrOfFission = tag.getFloat("nbrOfFission");
        additionalNeutronsAbsorbed = tag.getFloat("additionalNeutrons");
        temperature = tag.getFloat("temperature");
        power = tag.getFloat("power");
        super.read(tag, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        FormicApiLang.formatRadiationFlux(getRadioactiveActivity() * 20)
                .style(ChatFormatting.DARK_GREEN)
                .forGoggles(tooltip, 1);

        FormicApiLang.formatTemperature(temperature)
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);

        return true;
    }

    @Override
    public Couple<Float> absorbNeutrons(Couple<Float> radiationFlux) {
        Float temperatureCoef = 1 / Math.max(1, (temperature - 200) * CROWNSConfigs.SERVER.nuclear.negativeThermalCoef.getF());
        //System.out.println("temperature coef "+ temperatureCoef);
        float fastAbsorbed = 0f;
        float slowAbsorbed = 0f;
        for (ResourceLocation resourceLocation : radioactiveElements.keySet()) {
            Float massFrac = radioactiveElements.get(resourceLocation);
            Float cm = IAmFissileMaterial.molarConcentration.get(resourceLocation);
            Float fastAbsorptionChance = Math.min(1,
                    IAmFissileMaterial.fissileCrossSection.get(resourceLocation).getFirst()
                            * massFrac * cm * barnNa);
            Float slowAbsorptionChance = Math.min(1,
                    IAmFissileMaterial.fissileCrossSection.get(resourceLocation).getSecond()
                            * massFrac * cm * barnNa);
            //System.out.println(resourceLocation);
            //System.out.println("fastC : "+ fastAbsorptionChance);
            //System.out.println("slowC : "+ slowAbsorptionChance);
            fastAbsorbed += radiationFlux.getFirst() * temperatureCoef * fastAbsorptionChance;
            slowAbsorbed += radiationFlux.getSecond() * temperatureCoef * slowAbsorptionChance;
        }
        additionalNeutronsAbsorbed += fastAbsorbed + slowAbsorbed;
        return Couple.create(radiationFlux.getFirst() - fastAbsorbed, radiationFlux.getSecond() - slowAbsorbed);
    }

    public void setComposition(CompoundTag composition) {
        radioactiveElements = new HashMap<>();
        if (composition != null) {
            for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
                if (composition.contains(resourceLocation.toString())) {
                    float concentration = composition.getFloat(resourceLocation.toString());
                    radioactiveElements.put(resourceLocation, concentration);
                }
            }
        }
    }
}
