package com.rae.crowns.content.rendering.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

import static com.rae.crowns.Constants.maxLight;

public class SmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private double xd, yd, zd;
    private final float fadeStart;

    protected SmokeParticle(ClientLevel level, double x, double y, double z,
                            double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.sprites = sprites;
        this.quadSize = 0.5f;
        this.lifetime = 75;
        this.gravity = 0.0f;
        this.friction = 0.0f;
        this.hasPhysics = false;
        this.fadeStart = lifetime * 0.25f;
        this.pickSprite(sprites);

        float color = (float) Math.random() * 0.3f + 0.2f;

        this.rCol = color;
        this.gCol = color;
        this.bCol = color;
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

        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        this.xd *= 0.76;
        this.yd *= 0.76;
        this.zd *= 0.76;

        if (this.age > this.fadeStart) {
            float progress = (this.age - this.fadeStart) / (this.lifetime - this.fadeStart);
            this.alpha = 1.0f - progress;
        } else {
            this.alpha = 1.0f;
        }

        this.quadSize = 1f + (float) this.age / this.lifetime;

        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /*public int getLightColor(float partialTick) {
        return maxLight;
    }*/

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new SmokeParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}