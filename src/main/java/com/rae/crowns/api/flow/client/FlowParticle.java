package com.rae.crowns.api.flow.client;

import com.mojang.math.Vector3f;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.Vec3;

public class FlowParticle extends SimpleAnimatedParticle {
    private final FlowLine spline;
    private double t; // Parameter along the B-spline (0 to 1)

    public FlowParticle(ClientLevel world, FlowLine spline, float initialT, SpriteSet spriteSet) {
        super(world, 0, 0, 0, spriteSet, 0.0f);
        setSpriteFromAge(spriteSet);
        this.spline = spline;
        this.t = initialT;
        this.lifetime = 100;
        this.hasPhysics = false;
        // Set initial position based on B-spline
        Vec3 initialPosition = this.spline.getPoint(initialT);
        this.setPos(initialPosition.x(), initialPosition.y(), initialPosition.z());

        // Set particle size
        this.setSize(0.1f,0.1f);
    }

    @Override
    public void tick() {
        super.tick();

        // Get the speed at the current t value
        double speed = spline.getSpeedAtT(t);

        // Move along the spline based on the speed
        t += speed;
        if (t > 1.0) t = 1.0;

        // Get the current position along the B-spline
        Vec3 dPos = spline.getPoint((float) t).subtract(spline.getPoint((float) (t-speed)));

        // Update the particle's position
        this.move(dPos.x(), dPos.y(), dPos.z());

        // Get the current color from the B-spline and update the particle's color
        Color color = spline.getColorAtT(t);
        this.setColor(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat());

        // Remove the particle if it reaches the end of the spline
        if (t >= 1.0) {
            this.remove();
        }
    }
    public static class Factory implements ParticleProvider<FlowParticleData> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet animatedSprite) {
            this.spriteSet = animatedSprite;
        }

        @Override
        public Particle createParticle(FlowParticleData data, ClientLevel worldIn, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {

            return new FlowParticle(worldIn,data.getSpline(), (float) data.getInitialT(), this.spriteSet);
        }
    }
}