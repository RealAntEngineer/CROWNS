package com.rae.crowns.content.nuclear.packets;

import com.rae.crowns.init.misc.ParticleInit;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static org.joml.Math.clamp;

public class RenderExplosionPacket extends SimplePacketBase {
    private static final RandomSource rand = RandomSource.create();

    private final double x, y, z;
    private final int count;

    public RenderExplosionPacket(double x, double y, double z, int count) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
    }

    public RenderExplosionPacket(@NotNull FriendlyByteBuf buffer) {
        x = buffer.readDouble();
        y = buffer.readDouble();
        z = buffer.readDouble();
        count = buffer.readInt();
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeInt(count);
    }

    @Override
    public boolean handle(NetworkEvent.@NotNull Context context) {
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                ClientLevel level = Minecraft.getInstance().level;
                assert Minecraft.getInstance().level != null;

                for (int i = 0; i < count; i++) {
                    double vx = rand.nextGaussian() * (1 + count / 150.0);
                    double vy = rand.nextGaussian() * (1 + count / 100.0);
                    double vz = rand.nextGaussian() * (1 + count / 150.0);
                    if (rand.nextBoolean()) vy = Math.abs(vy);

                    double offsetX = (rand.nextDouble() - 0.5) * 1.5;
                    double offsetY = 0.1 + (rand.nextDouble() - 0.5);
                    double offsetZ = (rand.nextDouble() - 0.5) * 1.5;

                    level.addParticle(
                            ParticleInit.nukeBlast.get(),
                            this.x + (offsetX),
                            this.y + (offsetY),
                            this.z + (offsetZ),
                            vx * 0.6f, vy * 0.6f, vz * 0.6f
                    );
                }

                for (int i = 0; i < 300; i++) {
                    double offsetX = (rand.nextDouble() - 0.5) * 8.0f;
                    double offsetY = (rand.nextDouble() - 0.5) * 8.0f;
                    double offsetZ = (rand.nextDouble() - 0.5) * 8.0f;

                    double theta = rand.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * rand.nextDouble() - 1);
                    double dx = Math.sin(phi) * Math.cos(theta);
                    double dy = Math.sin(phi) * Math.sin(theta);
                    double dz = Math.cos(phi);

                    double speed = 0.1f;

                    level.addParticle(
                            ParticleTypes.CLOUD,
                            this.x + offsetX,
                            this.y + offsetY,
                            this.z + offsetZ,
                            dx * speed,
                            dy * speed,
                            dz * speed
                    );

                    speed = 5f;

                    level.addParticle(
                            ParticleInit.shockwave.get(),
                            this.x,
                            this.y,
                            this.z,
                            dx * speed,
                            dy * speed,
                            dz * speed
                    );
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
