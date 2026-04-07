package com.rae.crowns.content.rendering.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class ShockwaveParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private double vx, vy, vz;

    protected ShockwaveParticle(
            ClientLevel level,
            double x, double y, double z,
            double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);

        this.vx = xd;
        this.vy = yd;
        this.vz = zd;
        this.sprites = sprites;
        this.lifetime = 60;
        this.quadSize = 0.25f;
        this.gravity = 0.0f;
        this.friction = 1f;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.oRoll = this.roll;
        this.alpha = 0.5f * (1.0f - (float)this.age / (float)this.lifetime);
        this.quadSize = 0.25f + (0.4f - 0.25f) * ((float)this.age / (float)this.lifetime);
        this.rCol = 0.8f;
        this.gCol = 0.8f;
        this.bCol = 0.8f;

        this.x += this.vx;
        this.y += this.vy;
        this.z += this.vz;

        this.vx *= 0.95;
        this.vy *= 0.95;
        this.vz *= 0.95;

        this.setSpriteFromAge(this.sprites);
    }


    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;


        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                @NotNull SimpleParticleType type,
                @NotNull ClientLevel level,
                double x, double y, double z,
                double xd, double yd, double zd) {
            return new ShockwaveParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
