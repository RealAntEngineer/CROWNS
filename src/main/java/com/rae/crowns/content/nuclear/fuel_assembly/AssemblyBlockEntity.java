package com.rae.crowns.content.nuclear.fuel_assembly;

import com.rae.crowns.CROWNSLang;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.nuclear.Nucleus;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.misc.FluidInit;
import com.rae.crowns.init.misc.NucleusInit;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IHaveGoggleInformation {

    public float temperature = 300;
    public float C = 3000 * 200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly

    private final List<Nucleus> whitelist = List.of( // List of nuclei that are shown on goggle tooltip
            NucleusInit.U235,
            NucleusInit.U238,
            NucleusInit.Xe135,
            NucleusInit.Sr90
    );

    public HashMap<Nucleus, Float> inventory = new HashMap<>(); // Number of mol for each isotope

    public float receivingFastFlux = 0; // > 0.1eV
    public float receivingSlowFlux = 2f; // < 0.1eV

    public float outgoingFlux = 0; // Always fast!

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    public AssemblyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState state) {
        super(blockEntityType, blockPos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
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
        }

        // Simulation goes here
        for (Nucleus nucleus : inventory.keySet().stream().toList()) {
            float mol = inventory.get(nucleus);

            Nucleus.NuclearTransformationResult fast_result = nucleus.fission(receivingFastFlux, mol, 1f, 0.25f, true); // Fast spectrum
            Nucleus.NuclearTransformationResult thermal_result = nucleus.fission(receivingSlowFlux, mol, 1f, 0.25f, false); // Thermal spectrum
            Nucleus.NuclearTransformationResult decay_result = nucleus.decay(1f, mol);

            outgoingFlux += fast_result.neutron_yielded() + thermal_result.neutron_yielded() + decay_result.neutron_yielded();

            temperatureChange(fast_result.energy_yielded() + thermal_result.energy_yielded() + decay_result.energy_yielded());

            // Since it can return null elements, we should check for null before adding
            HashMap<Nucleus, Float> nullableElements = new HashMap<>();
            nullableElements.putAll(fast_result.elements()); nullableElements.putAll(thermal_result.elements()); nullableElements.putAll(decay_result.elements());

            nullableElements.forEach((tempNucleus, tempFloat) -> { // For some reason, I got the amazing idea to convert the elements to Optionals and check for null
                Optional<Nucleus> optionalNucleus = Optional.ofNullable(tempNucleus);
                Optional<Float> optionalFloat = Optional.ofNullable(tempFloat);

                optionalNucleus.ifPresent((presentNucleus) -> {
                    optionalFloat.ifPresent((presentFloat) -> inventory.put(presentNucleus, presentFloat));
                });
            });
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
        FormicApiLang.formatTemperature(temperature)
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);

        tooltip.add(Component.literal("Composition").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));

        for (Nucleus nucleus : inventory.keySet().stream().toList()) {
            if (!whitelist.contains(nucleus)) continue;

            String nucleusName = CROWNSLang.nucleus(nucleus).string();
            double concentration = nucleus.moleToMass(inventory.get(nucleus));

            tooltip.add(Component.translatable(nucleusName).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format(" : %.2f %%", concentration * 100)).withStyle(ChatFormatting.GRAY)));
        }

        return true;
    }

    @Override
    protected @NotNull AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }

    public void setComposition(@Nullable CompoundTag composition) {
        if (composition != null) { // If null we keep the default.
            inventory.clear();
            for (Nucleus nucleus : NucleusInit.allNuclei) {
                if (composition.contains(CROWNSLang.nucleus(nucleus).string())) {
                    double mol = nucleus.massToMole((float) composition.getDouble(CROWNSLang.nucleus(nucleus).string())); // Will refactor
                    inventory.put(nucleus, (float) mol);
                }
            }
        }
    }

    public @NotNull CompoundTag saveComposition() {
        CompoundTag composition = new CompoundTag();

        for (Nucleus nucleus : inventory.keySet().stream().toList()) {
            double concentration = nucleus.moleToMass(inventory.get(nucleus));
            String string = CROWNSLang.nucleus(nucleus).string();

            composition.putDouble(string, concentration);
        }

        return composition;
    }

    private void temperatureChange(double Q) {
        temperature += (float) Q / (3000 * C);
    }

    private void meltdown(@NotNull BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
        //level.removeBlockEntity(pos);
    }
}