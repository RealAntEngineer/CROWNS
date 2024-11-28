package com.rae.crowns.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rae.crowns.init.ParticleTypeInit;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;

public class SteamFlowParticleData implements ParticleOptions, ICustomParticleDataWithSprite<SteamFlowParticleData> {
	
	public static final Codec<SteamFlowParticleData> CODEC = RecordCodecBuilder.create(i ->
			i.group(
						Codec.FLOAT.fieldOf("rx").forGetter(p -> p.rotX),
						Codec.FLOAT.fieldOf("ry").forGetter(p -> p.rotY),
						Codec.FLOAT.fieldOf("rz").forGetter(p -> p.rotZ),
						Codec.FLOAT.fieldOf("ox").forGetter(p -> p.offX),
						Codec.FLOAT.fieldOf("oy").forGetter(p -> p.offY),
						Codec.FLOAT.fieldOf("oz").forGetter(p -> p.offZ)
				)
		.apply(i, SteamFlowParticleData::new));

	public static final Deserializer<SteamFlowParticleData> DESERIALIZER = new Deserializer<SteamFlowParticleData>() {
		public SteamFlowParticleData fromCommand(ParticleType<SteamFlowParticleData> particleTypeIn, StringReader reader)
				throws CommandSyntaxException {
			reader.expect(' ');
			float rx = reader.readFloat();
			reader.expect(' ');
			float ry = reader.readFloat();
			reader.expect(' ');
			float rz = reader.readFloat();
			reader.expect(' ');
			float ox = reader.readFloat();
			reader.expect(' ');
			float oy = reader.readFloat();
			reader.expect(' ');
			float oz = reader.readFloat();
			return new SteamFlowParticleData(rx, ry, rz,ox, oy, oz);
		}

		public SteamFlowParticleData fromNetwork(ParticleType<SteamFlowParticleData> particleTypeIn, FriendlyByteBuf buffer) {
			return new SteamFlowParticleData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}
	};

	final float rotX;
	final float rotY;
	final float rotZ;
	final float offX;
	final float offY;
	final float offZ;

	public SteamFlowParticleData(Vec3 rotatingDirection, Vec3 rotatingOffset) {
		this((float) rotatingDirection.x(), (float) rotatingDirection.y(), (float) rotatingDirection.z(), (float) rotatingOffset.x(), (float) rotatingOffset.y(), (float) rotatingOffset.z());
	}

	public SteamFlowParticleData(float rotX, float rotY, float rotZ,float offX,float offY, float offZ) {
		this.rotX = rotX;
		this.rotY = rotY;
		this.rotZ = rotZ;
		this.offX = offX;
		this.offY = offY;
		this.offZ = offZ;
	}

	public SteamFlowParticleData() {
		this(0f,0f,0f,0f,0f,0f);
	}


	@Override
	public ParticleType<?> getType() {
		return ParticleTypeInit.STEAM_FLOW.get();
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buffer) {
		buffer.writeFloat(rotX);
		buffer.writeFloat(rotY);
		buffer.writeFloat(rotZ);
		buffer.writeFloat(offX);
		buffer.writeFloat(offY);
		buffer.writeFloat(offZ);
	}

	@Override
	public String writeToString() {
		return String.format(Locale.ROOT, "%s %f %f %f %f %f %f", ParticleTypeInit.STEAM_FLOW.parameter(), rotX, rotY, rotZ,offX, offY, offZ);
	}

	@Override
	public Deserializer<SteamFlowParticleData> getDeserializer() {
		return DESERIALIZER;
	}
	
	@Override
	public Codec<SteamFlowParticleData> getCodec(ParticleType<SteamFlowParticleData> type) {
		return CODEC;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public SpriteParticleRegistration<SteamFlowParticleData> getMetaFactory() {
		return SteamFlowParticle.Factory::new;
	}

}