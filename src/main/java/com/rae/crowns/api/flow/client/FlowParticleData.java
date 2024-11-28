package com.rae.crowns.api.flow.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.rae.crowns.init.ParticleTypeInit;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class FlowParticleData implements ParticleOptions, ICustomParticleDataWithSprite<FlowParticleData> {
    // Codec for serialization and deserialization
    public static final Codec<FlowParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FlowLine.CODEC.fieldOf("spline").forGetter(FlowParticleData::getSpline),  // Using the BSpline codec
            Codec.DOUBLE.fieldOf("initialT").forGetter(FlowParticleData::getInitialT) // Codec for the initialT
    ).apply(instance, FlowParticleData::new));
    public static final Deserializer<FlowParticleData> DESERIALIZER = new Deserializer<>() {
        @Override
        public FlowParticleData fromCommand(ParticleType<FlowParticleData> particleType, StringReader reader) throws CommandSyntaxException {
            // Parsing command input (optional)
            reader.expect(' ');
            float x = reader.readFloat();
            reader.expect(' ');
            float y = reader.readFloat();
            reader.expect(' ');
            float z = reader.readFloat();
            return new FlowParticleData(new FlowLine(List.of(new Vec3(x,y,z)),List.of(1.0),List.of(Color.WHITE)), 0.0);
        }

        @Override
        public FlowParticleData fromNetwork(ParticleType<FlowParticleData> particleType, FriendlyByteBuf buffer) {
            // Deserialize the spline and initial 't' from network data
            FlowLine spline = FlowLine.readFromBuffer(buffer);
            double initialT = buffer.readDouble();
            return new FlowParticleData(spline, initialT);
        }
    };

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
    public void writeToNetwork(FriendlyByteBuf buffer) {
        // Serialize the spline and initial 't' for network transmission
        spline.writeToBuffer(buffer);
        buffer.writeDouble(initialT);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %s %f", ParticleTypeInit.FLOW_PARTICLE.parameter(), spline.toString(), initialT);
    }

    @Override
    public Deserializer<FlowParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public Codec<FlowParticleData> getCodec(ParticleType<FlowParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<FlowParticleData> getMetaFactory() {
        return FlowParticle.Factory::new;
    }
}