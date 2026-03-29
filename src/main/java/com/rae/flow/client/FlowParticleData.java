package com.rae.flow.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rae.flow.commun.FlowLine;
import com.rae.crowns.init.client.ParticleTypeInit;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FlowParticleData implements ParticleOptions, ICustomParticleDataWithSprite<FlowParticleData> {
    // Codec for serialization and deserialization
    public static final MapCodec<FlowParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlowLine.CODEC.fieldOf("spline").forGetter(FlowParticleData::getSpline),  // Using the BSpline codec
            Codec.DOUBLE.fieldOf("initialT").forGetter(FlowParticleData::getInitialT) // Codec for the initialT
    ).apply(instance, FlowParticleData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,FlowParticleData> STREAM_CODEC = StreamCodec
            .composite(
                    FlowLine.STREAM_CODEC, FlowParticleData::getSpline,
                    ByteBufCodecs.DOUBLE, FlowParticleData::getInitialT,
                    FlowParticleData::new
            );

    private final FlowLine spline;
    private final double initialT;
    public FlowParticleData(){
        this(new FlowLine(List.of(Vec3.ZERO, Vec3.ZERO.relative(Direction.NORTH,1f)),List.of(0.1d,0d),List.of(Color.WHITE,Color.WHITE)),0);
    }
    public FlowParticleData(FlowLine spline, double initialT) {
        this.spline = spline;
        this.initialT = initialT;
    }

    public FlowLine getSpline() {
        return spline;
    }

    public double getInitialT() {
        return initialT;
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleTypeInit.FLOW_PARTICLE.get();
    }

    @Override
    public MapCodec<FlowParticleData> getCodec(ParticleType<FlowParticleData> type) {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FlowParticleData> getStreamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<FlowParticleData> getMetaFactory() {
        return FlowParticle.Factory::new;
    }
}