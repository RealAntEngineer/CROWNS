package com.rae.crowns.content.nuclear.fuel_assembly;

import com.rae.crowns.CROWNS;
import com.rae.crowns.CROWNSLang;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.event.ServerEvents;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.hazards.radiation.pointsource.PointSourceUtil;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.nuclear.Nucleus;
import com.rae.crowns.content.nuclear.packets.RenderExplosionPacket;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.data.PacketInit;
import com.rae.crowns.init.misc.FluidInit;
import com.rae.crowns.init.misc.NucleusInit;
import com.rae.crowns.init.misc.ParticleInit;
import com.rae.crowns.init.misc.TagsInit;
import com.rae.formicapi.FormicAPI;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.rae.crowns.content.nuclear.NuclearExplosion.nuclearExplosion;
import static org.joml.Math.clamp;

public class AssemblyBlockEntity extends SmartBlockEntity implements IHaveTemperature, IHaveGoggleInformation {
    private static final Random r = new Random();
    private static final Logger log = LoggerFactory.getLogger(AssemblyBlockEntity.class);

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

    public float receivingFastFlux = 0f;//it's a neutron flux in mol.. Before it was in millions be aware !!!
    public float receivingSlowFlux = 0f;

    private float lastFastFlux = 0f;
    private float lastSlowFlux = 0f;

    public float outgoingFlux = 0; // Always fast!

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean                                queuedSync;
    private   HashMap<BlockPos, AssemblyBlockEntity> assemblies = new HashMap<>();

    private final @NotNull RandomSource random;

    public AssemblyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState state) {
        super(blockEntityType, blockPos, state);

        this.random = RandomSource.create();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();

        //ServerEvents.assemblies.put(this, getBlockPos());
    }

    @Override
    public void invalidate() {
        super.invalidate();

        //ServerEvents.assemblies.remove(this, getBlockPos());
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
            //TODO add the activity
            if (temperature > 900) {
                level.setBlock(origin, getBlockState().setValue(AssemblyBlock.TEMPERATURE, AssemblyBlock.Temperature.HOT), 3);
            } else if (temperature > 400) {
                level.setBlock(origin, getBlockState().setValue(AssemblyBlock.TEMPERATURE, AssemblyBlock.Temperature.WARM), 3);
            } else {
                level.setBlock(origin, getBlockState().setValue(AssemblyBlock.TEMPERATURE, AssemblyBlock.Temperature.COLD), 3);
            }
        }
        //TODO ONLY simulated on the server, we don't need it on the client

        // Simulation goes here
        boolean explosive = false;

        outgoingFlux = 0f;

        HashMap<Nucleus, Float> presentElements = new HashMap<>();

        for (Map.Entry<Nucleus, Float> e : inventory.entrySet()) {
            Nucleus nucleus = e.getKey();
            Float mol = e.getValue();

            float volume = (1 * (1 + CROWNSConfigs.SERVER.nuclear.negativeThermalCoef.getF() * (temperature - 300))); // How much the thingamajig "expands"

            Nucleus.NuclearTransformationResult fast_result = nucleus.fission(receivingFastFlux, mol, volume, 0.25f, true); // Fast spectrum
            Nucleus.NuclearTransformationResult thermal_result = nucleus.fission(receivingSlowFlux, mol, volume, 0.25f, false); // Thermal spectrum
            Nucleus.NuclearTransformationResult decay_result = nucleus.decay(1f, mol);

            outgoingFlux += fast_result.neutron_yielded() + thermal_result.neutron_yielded() + decay_result.neutron_yielded();

            double E = fast_result.energy_yielded() + thermal_result.energy_yielded() + decay_result.energy_yielded();
            //WARNING the energy yielded need to be multiplied by the coefficient in the constants
            temperatureChange(E);
            //TODO only explode if config activated
            if (temperature > 3422) meltdown(getBlockPos()); // Melting point of tungsten as placeholder
            if (E > 1e14) standardExplosion(getBlockPos(), 10);

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
            presentElements.merge(nucleus, -totalConsumed, Float::sum);
        }

        presentElements.forEach((nucleus, mol) -> {
            inventory.merge(nucleus, mol, Float::sum);
        }); // To avoid a ConcurrentModificationException

        // Neutron transport here

        assemblies.forEach((pos, be) -> {
            if (be == this) return;

            ArrayList<BlockPos> line = PointSourceUtil.getBresenhamLine(origin, pos);
            double moderationFactor = 0;
            boolean absorbed = false;

            line.remove(pos); // Make sure the assembly does not interact with itself

            for (BlockPos linePos : line) {
                BlockState blockState = level.getBlockState(linePos);
                if (blockState.getFluidState().is(FluidTags.WATER)) moderationFactor = 1 - (1 - moderationFactor) * 0.5;
                if (TagsInit.CustomBlockTags.COAL_BLOCK.matches(blockState)) moderationFactor = 1 - (1 - moderationFactor) * 0.2;
                if (TagsInit.CustomBlockTags.ABSORBER.matches(blockState)) { absorbed = true; break; }
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
        temperature -= temperatureDifference * CROWNSConfigs.SERVER.nuclear.heatLossCoef.getF();// HELLL NO
        //TODO freeze simulation if the temperature is not ticking
        //TODO right now the simulation can say if in the last ticking the chunk section got an update.
        // IF it's frozen due to server overload it will still say that it's ticking even if it's blocked
        // so need to give the last gametick the section got updated and freeze if it's

        if (Float.isNaN(temperature)) {
            temperature = 300;
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        BlockPos origin = getBlockPos();
        //TODO we could go even slower than that. every 2 seconds should be fast enough
        assemblies = PointSourceUtil.findAssemblies(origin, level, CROWNSConfigs.SERVER.nuclear.radiationRange.get().intValue());
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
                .append(FormicApiLang.formatTemperature(temperature).component()).withStyle(ChatFormatting.DARK_RED));

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

            String nucleusName = CROWNSLang.nucleus(nucleus).string();
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

            //TODO use a codec
            for (Nucleus nucleus : Nucleus.getAllValues()) {

                int key = nucleus.getId();

                if (composition.contains(String.valueOf(key))) {
                    double mol = composition.getDouble(String.valueOf(key));
                    inventory.put(nucleus, (float) mol);
                }
            }

            //add legacy composition

            for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
                if (composition.contains(resourceLocation.toString())) {
                    double concentration = composition.getDouble(resourceLocation.toString());
                    if (resourceLocation.equals(CROWNS.resource("u235"))) {
                        inventory.put(NucleusInit.U235, NucleusInit.U235.massToMole((float) (concentration*600_000)));
                    } else if (resourceLocation.equals(CROWNS.resource("u238"))) {
                        inventory.put(NucleusInit.U238,  NucleusInit.U238.massToMole((float) (concentration*600_000)));

                    }
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
        /*assemblies.clear(); // If any are removed

        ServerEvents.assemblies.forEach((be, pos) -> {
            double distance = pos.subtract(getBlockPos()).getCenter().length();
            if (distance > CROWNSConfigs.SERVER.nuclear.radiationRange.get()) return;

            assemblies.put(pos, be);
        });*/
    }

    private void temperatureChange(double Q) {
        float coEf = 0.75f * 1e4f; // Change this for how much you want the temperature increase to slow down
        temperature += (float) Q / (C * coEf);
    }

    private void standardExplosion(@NotNull BlockPos pos, float power) {
        assert this.level != null;

        // TODO: transition to NuclearExplosion.java

        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);

        if (!level.isClientSide()) {
            RenderExplosionPacket packet = new RenderExplosionPacket(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    25
            );

            PacketInit.getChannel().send(
                    PacketDistributor.NEAR.with(
                            PacketDistributor.TargetPoint.p(
                                    pos.getX() + 0.5,
                                    pos.getY() + 0.5,
                                    pos.getZ() + 0.5,
                                    64,
                                    level.dimension()
                            )
                    ),
                    packet
            );
        }
    }

    private void meltdown(@NotNull BlockPos pos) {
        assert level != null;
        level.setBlockAndUpdate(pos, FluidInit.CORIUM.get().getFlowing(8, 15, false).createLegacyBlock());
        level.removeBlockEntity(pos);
    }
}