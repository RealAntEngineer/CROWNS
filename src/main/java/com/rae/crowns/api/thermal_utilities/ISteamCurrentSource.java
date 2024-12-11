package com.rae.crowns.api.thermal_utilities;

import com.rae.crowns.content.legacy.LegacySteamCurrent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

@MethodsReturnNonnullByDefault
public interface ISteamCurrentSource {
	@Nullable
    LegacySteamCurrent getSteamCurrent();

	@Nullable
	Level getSteamCurrentWorld();

	BlockPos getSteamCurrentPos();
	float getSpeed();

	Direction getSteamFlowOriginSide();

	//@Nullable
	//Direction getSteamFlowDirection();

	default float getMaxDistance() {
		/*float speed = Math.abs(this.getSpeed());
		CKinetics config = AllConfigs.server().kinetics;
		float distanceFactor = Math.min(speed / config.fanRotationArgmax.get(), 1);
		float pushDistance = Mth.lerp(distanceFactor, 3, config.fanPushDistance.get());
		float pullDistance = Mth.lerp(distanceFactor, 3f, config.fanPullDistance.get());
		return this.getSpeed() > 0 ? pushDistance : pullDistance;*/
		return 10;
	}

	boolean isSourceRemoved();
}
