package com.rae.crowns.content.thermodynamics.turbine;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class SteamFlowParticle extends SimpleAnimatedParticle {
	private final Vec3 rotatingDirection;
	private float rotatedAngle = 0;
	private Vec3 rotatingOffset;

	protected SteamFlowParticle(ClientLevel world,Vec3 rotatingDirection,Vec3 rotatingOffset, double x, double y, double z,
								SpriteSet sprite) {
		super(world, x, y, z, sprite, world.random.nextFloat() * .5f);
		this.rotatingDirection = rotatingDirection;
		this.rotatingOffset = rotatingOffset;
		this.quadSize *= 1.75F;
		this.lifetime = 100;
		hasPhysics = false;
		selectSprite(7);
		Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .25f);
		this.setPos(x + offset.x, y + offset.y, z + offset.z);
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		setColor(0xBBEEEE);
		setAlpha(0.5f);
	}

	@Nonnull
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			remove();
		} else {
			Vec3 motion = rotatingDirection.scale(1 / 8f);
			float dAngle = 0.05f;
			Vec3 rotation = getRotation(rotatedAngle+dAngle).subtract(getRotation(rotatedAngle));
			motion = motion.add(rotation);
			xd = motion.x;
			yd = motion.y;
			zd = motion.z;

			if (this.onGround) {
				this.xd *= 0.7;
				this.zd *= 0.7;
			}
			this.move(this.xd, this.yd, this.zd);
			rotatedAngle += dAngle;
		}
	}

	@NotNull
	private Vec3 getRotation(float rotatedAngle) {
        return rotatingOffset
				.xRot((float) rotatingDirection.x*rotatedAngle)
				.yRot((float) rotatingDirection.y*rotatedAngle)
				.zRot((float) rotatingDirection.z*rotatedAngle);
	}


	public int getLightColor(float partialTick) {
		BlockPos blockpos = new BlockPos(this.x, this.y, this.z);
		return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(level, blockpos) : 0;
	}

	private void selectSprite(int index) {
		setSprite(sprites.get(index, 8));
	}
	public static class Factory implements ParticleProvider<SteamFlowParticleData> {
		private final SpriteSet spriteSet;

		public Factory(SpriteSet animatedSprite) {
			this.spriteSet = animatedSprite;
		}

		@Override
		public Particle createParticle(SteamFlowParticleData data, ClientLevel worldIn, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {

			return new SteamFlowParticle(worldIn,new Vec3(data.rotX,data.rotY,data.rotZ),new Vec3(data.offX,data.offY,data.offZ), x, y, z, this.spriteSet);
		}
	}

}
