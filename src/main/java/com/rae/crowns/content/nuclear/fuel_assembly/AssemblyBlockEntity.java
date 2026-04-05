package com.rae.crowns.content.nuclear.fuel_assembly;

import com.rae.crowns.CROWNSLang;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.event.ServerEvents;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.hazards.radiation.pointsource.PointSourceUtil;
import com.rae.crowns.content.nuclear.Nucleus;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.misc.FluidInit;
import com.rae.crowns.init.misc.NucleusInit;
import com.rae.crowns.init.misc.TagsInit;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IHaveGoggleInformation {
    private static final Random r = new Random();

    // TODO: Refactor the entire class

    public float temperature = 300;
    public float C = 3000 * 200; //specific thermal capacity J.K-1 it's a 3 ton metal assembly

    private final List<Integer> whitelist = List.of( // List of nuclei that are shown on goggle tooltip
            NucleusInit.U235.getId(),
            NucleusInit.U238.getId(),
            NucleusInit.Xe135.getId(),
            NucleusInit.Sr90.getId(),
            NucleusInit.Cs137.getId(),

            NucleusInit.Am241Be.getId(),
            NucleusInit.Be9.getId(),

            NucleusInit.Np237.getId(),

            NucleusInit.Cf252.getId(),

            NucleusInit.Pu239.getId(),
            NucleusInit.U239.getId(),
            NucleusInit.Np239.getId()
    );

    public HashMap<Nucleus, Float> inventory = new HashMap<>(); // Number of mol for each isotope

    public float receivingFastFlux = 0f;
    public float receivingSlowFlux = 0f;

    private float lastFastFlux = 0f;
    private float lastSlowFlux = 0f;

    public float outgoingFlux = 0; // Always fast!

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean                                queuedSync;
    private   HashMap<BlockPos, AssemblyBlockEntity> assemblies;

    public AssemblyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState state) {
        super(blockEntityType, blockPos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();

        assert level != null;
        if (!level.isClientSide()) {
            ServerEvents.assemblies.put(this, getBlockPos());
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (level != null && !level.isClientSide()) {
            ServerEvents.assemblies.remove(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        BlockPos origin = getBlockPos();

        if (!level.isClientSide()) {
            PhysicsWorldData data = PhysicsSaveManager.get((ServerLevel) level);

            if (data != null && !data
                    .ticked(SectionPos.of(getBlockPos()).asLong())) return;
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }
        } else {
            if (temperature > 900) {
                level.setBlock(origin, getBlockState().setValue(AssemblyBlock.TEMPERATURE, AssemblyBlock.Temperature.HOT), 3);
            } else if (temperature > 400) {
                level.setBlock(origin, getBlockState().setValue(AssemblyBlock.TEMPERATURE, AssemblyBlock.Temperature.WARM), 3);
            } else {
                level.setBlock(origin, getBlockState().setValue(AssemblyBlock.TEMPERATURE, AssemblyBlock.Temperature.COLD), 3);
            }
        }

        // Simulation goes here
        outgoingFlux = 0f;

        HashMap<Nucleus, Float> presentElements = new HashMap<>();

        for (Map.Entry<Nucleus, Float> e : inventory.entrySet()) {
            Nucleus key = e.getKey();
            Float value = e.getValue();

            float volume = (1 * (1 + CROWNSConfigs.SERVER.nuclear.negativeThermalCoef.getF() * (temperature - 300))); // How much the thingamajig "expands"

            Nucleus.NuclearTransformationResult fast_result = key.fission(receivingFastFlux, value, volume, 0.25f, true); // Fast spectrum
            Nucleus.NuclearTransformationResult thermal_result = key.fission(receivingSlowFlux, value, volume, 0.25f, false); // Thermal spectrum
            Nucleus.NuclearTransformationResult decay_result = key.decay(1f, value);

            outgoingFlux += fast_result.neutron_yielded() + thermal_result.neutron_yielded() + decay_result.neutron_yielded();

            temperatureChange(fast_result.energy_yielded() + thermal_result.energy_yielded() + decay_result.energy_yielded());

            // Since it can return null elements, we should check for null before adding
            HashMap<Nucleus, Float> nullableElements = new HashMap<>();
            nullableElements.putAll(fast_result.elements());
            nullableElements.putAll(thermal_result.elements());
            nullableElements.putAll(decay_result.elements());

            for (Map.Entry<Nucleus, Float> entry : nullableElements.entrySet()) {
                Nucleus tempNucleus = entry.getKey();
                Float tempFloat = entry.getValue();

                if (tempNucleus != null && tempFloat != null) {
                    presentElements.merge(tempNucleus, tempFloat, Float::sum);
                }
            }

            float totalConsumed = fast_result.consumed() + thermal_result.consumed() + decay_result.consumed();
            presentElements.merge(key, -totalConsumed, Float::sum);
        }

        presentElements.forEach((nucleus, mol) -> {
            inventory.merge(nucleus, mol, Float::sum);
        }); // To avoid a ConcurrentModificationException

        // Neutron transport here
        getAssemblies();

        assemblies.forEach((pos, be) -> {
            if (be == this) return;

            ArrayList<BlockPos> line = PointSourceUtil.getBresenhamLine(origin, pos);
            double moderationFactor = 0;
            boolean absorbed = false;

            line.remove(pos); // Make sure the assembly does not interact with itself

            for (BlockPos linePos : line) {
                if (level.getFluidState(linePos).is(FluidTags.WATER)) moderationFactor = 1 - (1 - moderationFactor) * 0.5;
                if (TagsInit.CustomBlockTags.COAL_BLOCK.matches(level.getBlockState(linePos))) moderationFactor = 1 - (1 - moderationFactor) * 0.2;
                if (TagsInit.CustomBlockTags.ABSORBER.matches(level.getBlockState(linePos))) { absorbed = true; break; }
            }

            if (absorbed) return;

            Vec3 vecDistance = origin.getCenter().subtract(pos.getCenter());
            double distance = vecDistance.length();

            double intensity = (1 / (distance * distance)); // Add a bit of randomness for spice

            float thermalFlux = (float) moderationFactor * outgoingFlux;
            float fastFlux = outgoingFlux - thermalFlux;

            be.receivingSlowFlux += (float)(thermalFlux * intensity);
            be.receivingFastFlux += (float)(fastFlux * intensity);
        });

        lastFastFlux = receivingFastFlux;
        lastSlowFlux = receivingSlowFlux;

        receivingFastFlux = 0; receivingSlowFlux = 0;

        float temperatureDifference = temperature - 300;
        temperature -= temperatureDifference * CROWNSConfigs.SERVER.nuclear.heatLossCoef.getF();

        if (Float.isNaN(temperature)) {
            temperature = 300;
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        /*BlockPos origin = getBlockPos();
        assemblies = PointSourceUtil.findAssemblies(origin, level, CROWNSConfigs.SERVER.nuclear.radiationRange.get().intValue());
        */
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
    protected void read(@NotNull CompoundTag tag, boolean clientPacket) {
        receivingFastFlux = tag.getFloat("receivingFastFlux");
        receivingSlowFlux = tag.getFloat("receivingSlowFlux");
        outgoingFlux = tag.getFloat("outgoingFlux");
        setComposition(tag.getCompound("composition"));
        temperature = tag.getFloat("temperature");
        super.read(tag, clientPacket);
    }

    @Override
    protected void write(@NotNull CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        tag.putFloat("receivingFastFlux", receivingFastFlux);
        tag.putFloat("receivingSlowFlux", receivingSlowFlux);
        tag.putFloat("outgoingFlux", outgoingFlux);
        tag.put("composition", saveComposition());
        tag.putFloat("temperature", temperature);
    }

    @Override
    public void writeSafe(@NotNull CompoundTag tag) {
        super.writeSafe(tag);

        tag.putFloat("receivingFastFlux", receivingFastFlux);
        tag.putFloat("receivingSlowFlux", receivingSlowFlux);
        tag.putFloat("outgoingFlux", outgoingFlux);
        tag.put("composition", saveComposition());
        tag.putFloat("temperature", temperature);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
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
        tooltip.add(Component.literal(""));

        tooltip.add(Component.literal("Temperature:").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(String.format(" : %.2f°C", temperature - 273.15)).withStyle(ChatFormatting.DARK_RED)));

        tooltip.add(Component.literal(""));

        tooltip.add(Component.literal("Thermal flux:").withStyle(ChatFormatting.RED)
                .append(Component.literal(String.format(" : %.5f/s", lastSlowFlux * 20)).withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal("Fast flux:").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format(" : %.5f/s", lastFastFlux * 20)).withStyle(ChatFormatting.AQUA)));

        tooltip.add(Component.literal("Outgoing flux:").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format(" : %.5f/s", outgoingFlux * 20)).withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal(""));

        tooltip.add(Component.literal("Composition:").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));

        inventory.forEach((nucleus, mol) -> {
            if (!whitelist.contains(nucleus.getId())) return;

            String nucleusName = CROWNSLang.readableNucleus(nucleus).string();
            double mass = nucleus.moleToMass(mol);
            double concentration = mass / 3000;

            if (concentration * 100 < 0.001) return;

            tooltip.add(Component.literal("  " + nucleusName).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format(" : %.2f%%", concentration * 100)).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("% .4fmol", mol)).withStyle(ChatFormatting.GRAY))
            );
        });

        return true;
    }

    @Override
    protected @NotNull AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }


    public void setComposition(@Nullable CompoundTag composition) {
        if (composition != null) { // If null, keep the default
            inventory.clear();

            for (Nucleus nucleus : NucleusInit.allNuclei) {
                int key = nucleus.getId();

                if (composition.contains(String.valueOf(key))) {
                    double mol = composition.getDouble(String.valueOf(key));
                    inventory.put(nucleus, (float) mol);
                }
            }
        }
    }

    public @NotNull CompoundTag saveComposition() {
        CompoundTag composition = new CompoundTag();

        for (Map.Entry<Nucleus, Float> entry : inventory.entrySet()) {
            Nucleus nucleus = entry.getKey();
            double mol = entry.getValue();

            int key = nucleus.getId();
            composition.putDouble(String.valueOf(key), mol);
        }

        return composition;
    }

    private void getAssemblies() {
        assemblies.clear(); // If any are removed

        ServerEvents.assemblies.forEach((be, pos) -> {
            double distance = pos.subtract(getBlockPos()).getCenter().length();
            if (distance > CROWNSConfigs.SERVER.nuclear.radiationRange.get()) return;

            assemblies.put(pos, be);
        });
    }

    private void temperatureChange(double Q) {
        float coEf = 1e4f; // Change this for how much you want the temperature increase to slow down
        temperature += (float) Q / (C * coEf);
    }

    private void meltdown(@NotNull BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
        //level.removeBlockEntity(pos);
    }
}