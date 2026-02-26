package com.rae.crowns.content.nuclear.fuel_assembly;

import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.nuclear.IAmRadioactiveSource;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.misc.FluidInit;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rae.crowns.Constants.*;
import static com.rae.crowns.content.nuclear.NuclearExplosion.nuclearExplosion;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IAmRadioactiveSource, IAmFissileMaterial, IHaveGoggleInformation {

    private static final int SYNC_RATE = 8;
    static int rayCount = 0;
    private static float negativeThermalCoef = 0.0075f;
    private final int LAZY_TICK_RATE = 5;
    public float temperature = 300;
    public float backgroundActivity = 12 * 3;//In MBq ( giga becquerels ) uranium is 12 Mbq per tonnes
    public float oldNbrOfFission;
    public float nbrOfFission;//nbr of fission/t
    public float C = 3000 * 200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly
    public @NotNull LerpedFloat additionalNeutronsAbsorbed = LerpedFloat.linear();
    public @NotNull HashMap<ResourceLocation, Double> radioactiveElements = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"), 0.014 * 0.2,
                    CROWNS.resource("u238"), 0.986 * 0.2,
                    CROWNS.resource("p239"), 0.00 * 0.2
            ));//for U235,U358 and Plutonium -> percentage of total mass
    protected int syncCooldown;

    //RGBAVolumeInstance tcherenkov;
    /*private void initializeClientTcherenkov() {
        // Create simple 16^3 blue cube with density scaling with distance and centered in 0.5,0.5,0.5
        int Nx = 4, Ny = 4, Nz = 4;
        int brickSize = 4;
        float[] volumeRGBA = new float[Nx * Ny * Nz * 4];

        for (int z = 0; z < Nz; z++) {
            float fz = (z + 0.5f) / Nz - 0.5f; // center at 8
            for (int y = 0; y < Ny; y++) {
                float fy = (y + 0.5f) / Ny - 0.5f; // center at 8
                for (int x = 0; x < Nx; x++) {
                    float fx = (x + 0.5f) / Nx - 0.5f; // center at 8

                    //float scaling = (1+3 * 0.25f)/(1+fz*fz + fy*fy + fx*fx);//Mth.clamp((float) Math.cos((fz*fz + fy*fy + fx*fx)/(3 * 0.25)/Math.PI/2* 2), 0, 1)*0.5f;
                    int idx = x + y*Nx + z*Nx*Ny;
                    int off = idx * 4;

                    if (fx * fx < 0.25 * 0.25 && fy * fy < 0.25 * 0.25 && fz * fz < 0.25 * 0.25){
                        volumeRGBA[off] = 0f; // R
                        volumeRGBA[off + 1] = 0f; // G
                        volumeRGBA[off + 2] = 0;       // B
                        volumeRGBA[off + 3] = 0f;        // A

                    } else {
                        volumeRGBA[off] = 0.2f; // R
                        volumeRGBA[off + 1] = 0.67f; // G
                        volumeRGBA[off + 2] = 0.9f;       // B
                        volumeRGBA[off + 3] = 0.03f;//1f;        // A

                    }
                }
            }
        }


        tcherenkov = new RGBAVolumeInstance(volumeRGBA, Nx, Ny, Nz, brickSize);
        tcherenkov.position = new Vec3(worldPosition.getX()-0.5, worldPosition.getY()-0.5, worldPosition.getZ()-0.5);//start pos
        tcherenkov.size = new Vec3(2.0, 2.0, 2.0);
        VolumeWorldRenderer.add(tcherenkov);
    }*/
    protected boolean queuedSync;
    float power = 0;
    double fastAbsorptionChance;
    double slowAbsorptionChance;
    int lastLazy = 0;

    public AssemblyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState state) {
        super(blockEntityType, blockPos, state);
        nbrOfFission = backgroundActivity;
        setLazyTickRate(LAZY_TICK_RATE);


    }

    public static void resetRayCounter() {
        //System.out.println(rayCount);
        rayCount = 0;
    }

    public static void reloadConfig() {
        negativeThermalCoef = CROWNSConfigs.SERVER.nuclear.negativeThermalCoef.getF();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && level.isClientSide) {
            this.lazyTickCounter = Math.toIntExact(LAZY_TICK_RATE - level.getGameTime() % LAZY_TICK_RATE);
        }
    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (!level.isClientSide()) {
            PhysicsWorldData data = PhysicsSaveManager.get((ServerLevel) level);

            if (data != null && !data
                    .ticked(SectionPos.of(getBlockPos()).asLong())) return;
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }

            if (CROWNSConfigs.COMMON.nuclearParticle.get())
                spawnRadiationParticles(level, getBlockPos(), nbrOfFission);
            temperature += power / C * 1 / 20f;
            additionalNeutronsAbsorbed.tickChaser();
            if (Math.toIntExact(level.getGameTime() % LAZY_TICK_RATE) == 0) {
                fakeLazyTick();
            }
        }

        if (Float.isNaN(temperature)) {
            temperature = 300;
        }
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

    public void spawnRadiationParticles(Level level, @NotNull BlockPos pos, float nbrOfFission) {
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

    public void fakeLazyTick() {
        assert level != null;
        if (!level.isClientSide()) {
            PhysicsWorldData data = PhysicsSaveManager.get((ServerLevel) level);
            if (data == null || !data.ticked(SectionPos.of(getBlockPos()).asLong())) return;
            oldNbrOfFission = nbrOfFission;
            nbrOfFission = additionalNeutronsAbsorbed.getValue() + backgroundActivity; //for now a 100% change of fission : no absorption
            //this is fine here. because
            if (Float.isNaN(nbrOfFission)) {
                nbrOfFission = backgroundActivity;
            }
            //warning. it get impacted by the other blocks during ImpactEnv, not itself. It would be better if the neutron
            // absorbed decay after lazy tick. here we are resting the goal every lazy tick. which means that if there is a block that impact us
            // and that tick before use it get erased
            BlockPos pos = getBlockPos();

            //float thermal_loses = (temperature-300)*10;// ambient temperature = 300K make thermal loses in the conduct temperature

            power = nbrOfFission * fissionEnergy * realismCoefficient;// - thermal_loses;

            //temperature += power/C;


            if (temperature > 3500 && CROWNSConfigs.SERVER.nuclear.explosion.get()) {
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
            rayCount += moreOptimizedImpactEnv(pos, level, CROWNSConfigs.SERVER.nuclear.radiationRange.get());

            notifyUpdate();

        }
    }

    private void standardExplosion(@NotNull BlockPos pos, float power) {
        assert this.level != null;
        nuclearExplosion(this.level, pos, power);
        // Remove the block after the explosion
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
    }

    private void meltdown(@NotNull BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
        //level.removeBlockEntity(pos);
    }

    @Override
    protected void write(@NotNull CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        tag.putFloat("nbrOfFission", nbrOfFission);
        tag.putFloat("additionalNeutrons", additionalNeutronsAbsorbed.getValue());
        tag.putFloat("temperature", temperature);
        tag.putFloat("power", power);
        tag.put("composition", saveComposition());
        tag.putInt("lastLazy", lastLazy);

    }

    @Override
    public void writeSafe(@NotNull CompoundTag tag) {
        super.writeSafe(tag);

        tag.putFloat("nbrOfFission", nbrOfFission);
        tag.putFloat("additionalNeutrons", additionalNeutronsAbsorbed.getValue());
        tag.putFloat("temperature", temperature);
        tag.putFloat("power", power);
        tag.put("composition", saveComposition());
        tag.putInt("lastLazy", lastLazy);
    }

    @Override
    protected void read(@NotNull CompoundTag tag, boolean clientPacket) {

        nbrOfFission = tag.getFloat("nbrOfFission");
        additionalNeutronsAbsorbed.startWithValue(tag.getFloat("additionalNeutrons"));
        temperature = tag.getFloat("temperature");
        power = tag.getFloat("power");
        lastLazy = tag.getInt("lastLazy");
        setComposition(tag.getCompound("composition"));
        super.read(tag, clientPacket);
    }

    public void setComposition(@Nullable CompoundTag composition) {
        if (composition != null) {//if null we keep the default.
            radioactiveElements = new HashMap<>();
            for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
                if (composition.contains(resourceLocation.toString())) {
                    double concentration = composition.getDouble(resourceLocation.toString());
                    radioactiveElements.put(resourceLocation, concentration);
                }
            }
        }
        computeAbsorptionChances();
    }

    public void computeAbsorptionChances() {
        fastAbsorptionChance = 0;
        slowAbsorptionChance = 0;
        for (ResourceLocation resourceLocation : radioactiveElements.keySet()) {
            double massFrac = radioactiveElements.get(resourceLocation);
            float cm = IAmFissileMaterial.molarConcentration.get(resourceLocation);
            fastAbsorptionChance += Math.min(1,
                    IAmFissileMaterial.fissileCrossSection.get(resourceLocation).getFirst()
                            * massFrac * cm * barnNa);
            slowAbsorptionChance += Math.min(1,
                    IAmFissileMaterial.fissileCrossSection.get(resourceLocation).getSecond()
                            * massFrac * cm * barnNa);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        //VolumeWorldRenderer.remove(tcherenkov);
        //tcherenkov = null;//will get garbage collected
    }

    public @NotNull CompoundTag saveComposition() {
        CompoundTag composition = new CompoundTag();
        for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
            if (radioactiveElements.containsKey(resourceLocation)) {
                double concentration = radioactiveElements.get(resourceLocation);

                composition.putDouble(resourceLocation.toString(), concentration);
            }
        }
        return composition;
    }

    @Override
    public float getThermalCapacity() {
        return C;
    }

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
    public boolean addToGoggleTooltip(@NotNull List<Component> tooltip, boolean isPlayerSneaking) {

        FormicApiLang.formatRadiationFlux(getRadioactiveActivity() * 20)
                .style(ChatFormatting.DARK_GREEN)
                .forGoggles(tooltip, 1);

        FormicApiLang.formatTemperature(temperature)
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);


        tooltip.add(Component.literal("composition").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
        for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
            if (radioactiveElements.containsKey(resourceLocation)) {
                double concentration = radioactiveElements.get(resourceLocation);
                tooltip.add(
                        Component.translatable(resourceLocation.toLanguageKey("nucleus")).withStyle(ChatFormatting.YELLOW)
                                .append(Component.literal(String.format(" : %.2f %%", concentration * 100)).withStyle(ChatFormatting.GRAY)));
            }
        }

        return true;
    }

    @Override
    public float getRadioactiveActivity() {
        float easeCoef = CROWNSConfigs.SERVER.nuclear.neutronFluxMultiplicator.getF(); //TODO config
        return backgroundActivity + nbrOfFission * 2.5f * easeCoef;
    }

    //maybe replace with an array so it's more memory efficient
    @Override
    public @NotNull Couple<Float> absorbNeutrons(@NotNull Couple<Float> radiationFlux) {
        int oldLast = lastLazy;//needed cause it could be hit by ray coming from a block that tick before us
        lastLazy = Math.toIntExact(level.getGameTime() / LAZY_TICK_RATE);
        if (oldLast != lastLazy) {//detect change of lazy tick.
            additionalNeutronsAbsorbed.chaseTimed(0, LAZY_TICK_RATE);
        }

        float temperatureCoef = 1 / Math.max(1, (temperature - 200) * negativeThermalCoef);
        //System.out.println("temperature coef "+ temperatureCoef);
        double fastAbsorbed = radiationFlux.getFirst() * temperatureCoef * fastAbsorptionChance;
        double slowAbsorbed = radiationFlux.getSecond() * temperatureCoef * slowAbsorptionChance;

        additionalNeutronsAbsorbed.chaseTimed(additionalNeutronsAbsorbed.getChaseTarget() + fastAbsorbed + slowAbsorbed,
                LAZY_TICK_RATE);
        return Couple.create((float) (radiationFlux.getFirst() - fastAbsorbed), (float) (radiationFlux.getSecond() - slowAbsorbed));
    }

    @Override
    public float getEffectiveK() {
        float easeCoef = CROWNSConfigs.SERVER.nuclear.neutronFluxMultiplicator.getF(); //TODO config
        return (backgroundActivity + nbrOfFission * 2.5f * easeCoef) / (backgroundActivity + oldNbrOfFission * 2.5f * easeCoef);
    }

    @Override
    protected @NotNull AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }
}