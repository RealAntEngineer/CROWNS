package com.rae.crowns.content.sound;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class SoundScape {
    private final float pitch;
    private final CrownsSoundScapes.AmbienceGroup group;
    private final CrownsSoundScapes.PitchGroup pitchGroup;
    List<ContinuousSound> continuous;
    List<RepeatingSound> repeating;
    private @Nullable Vec3 meanPos;

    public SoundScape(float pitch, CrownsSoundScapes.AmbienceGroup group) {
        this.pitchGroup = CrownsSoundScapes.getGroupFromPitch(pitch);
        this.pitch = pitch;
        this.group = group;
        continuous = new ArrayList<>();
        repeating = new ArrayList<>();
    }

    public SoundScape continuous(SoundEvent sound, float relativeVolume, float relativePitch) {
        return add(new ContinuousSound(sound, this, pitch * relativePitch, relativeVolume));
    }

    public @NotNull SoundScape add(ContinuousSound continuousSound) {
        continuous.add(continuousSound);
        return this;
    }

    public SoundScape repeating(SoundEvent sound, float relativeVolume, float relativePitch, int delay) {
        return add(new RepeatingSound(sound, this, pitch * relativePitch, relativeVolume, delay));
    }

    public @NotNull SoundScape add(RepeatingSound repeatingSound) {
        repeating.add(repeatingSound);
        return this;
    }

    public void play() {
        continuous.forEach(Minecraft.getInstance()
                .getSoundManager()::play);
    }

    public void tick() {
        if (AnimationTickHolder.getTicks() % CrownsSoundScapes.UPDATE_INTERVAL == 0)
            meanPos = null;
        repeating.forEach(RepeatingSound::tick);
    }

    public void remove() {
        continuous.forEach(ContinuousSound::remove);
    }

    public float getVolume() {
        Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
        float distanceMultiplier = 0;
        if (renderViewEntity != null) {
            double distanceTo = renderViewEntity.position()
                    .distanceTo(getMeanPos());
            distanceMultiplier = (float) Mth.lerp(distanceTo / CrownsSoundScapes.MAX_AMBIENT_SOURCE_DISTANCE, 2, 0);
        }
        int soundCount = CrownsSoundScapes.getSoundCount(group, pitchGroup);
        float max = AllConfigs.client().ambientVolumeCap.getF();
        float argMax = (float) CrownsSoundScapes.SOUND_VOLUME_ARG_MAX;
        return Mth.clamp(soundCount / (argMax * 10f), 0.025f, max) * distanceMultiplier;
    }

    public Vec3 getMeanPos() {
        return meanPos == null ? meanPos = determineMeanPos() : meanPos;
    }

    private Vec3 determineMeanPos() {
        meanPos = Vec3.ZERO;
        int amount = 0;
        for (BlockPos blockPos : CrownsSoundScapes.getAllLocations(group, pitchGroup)) {
            meanPos = meanPos.add(VecHelper.getCenterOf(blockPos));
            amount++;
        }
        if (amount == 0)
            return meanPos;
        return meanPos.scale(1f / amount);
    }

}
