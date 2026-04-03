package com.rae.crowns.content.nuclear.fuel_assembly;

import com.rae.crowns.CROWNS;
import com.rae.crowns.CROWNSLang;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.nuclear.IAmRadioactiveSource;
import com.rae.crowns.content.nuclear.Nucleus;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.misc.FluidInit;
import com.rae.crowns.init.misc.NucleusInit;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rae.crowns.Constants.*;
import static com.rae.crowns.content.nuclear.NuclearExplosion.nuclearExplosion;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IHaveGoggleInformation {

    public float temperature = 300;
    public float C = 3000 * 200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly

    public HashMap<Nucleus, Float> inventory = new HashMap<>(); // Number of mol for each isotope

    public float fastFlux = 0; // > 0.1eV
    public float slowFlux = 0; // < 0.1eV

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
        if (level !=  null && !level.isClientSide) {
            inventory.put(NucleusInit.U235, 10f);
            inventory.put(NucleusInit.U238, 10f);
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
        }

        temperature = 300;

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

    private void meltdown(@NotNull BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
        //level.removeBlockEntity(pos);
    }

    public void setComposition(@Nullable CompoundTag composition) {
        if (composition != null) { // if null we keep the default.
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

    @Override
    protected void read(@NotNull CompoundTag tag, boolean clientPacket) {
        fastFlux = tag.getFloat("fastFlux");
        slowFlux = tag.getFloat("slowFlux");
        setComposition(tag.getCompound("composition"));
        temperature = tag.getFloat("temperature");
        super.read(tag, clientPacket);
    }

    @Override
    protected void write(@NotNull CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        tag.putFloat("fastFlux", fastFlux);
        tag.putFloat("slowFlux", slowFlux);
        tag.put("composition", saveComposition());
        tag.putFloat("temperature", temperature);
    }

    @Override
    public void writeSafe(@NotNull CompoundTag tag) {
        super.writeSafe(tag);

        tag.putFloat("fastFlux", fastFlux);
        tag.putFloat("slowFlux", slowFlux);
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
            String nucleusName = CROWNSLang.nucleus(nucleus).string();
            double concentration = nucleus.moleToMass(inventory.get(nucleus));

            tooltip.add(Component.literal(nucleusName).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format(" : %.2f %%", concentration * 100)).withStyle(ChatFormatting.GRAY)));
        }

        return true;
    }

    @Override
    protected @NotNull AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }
}