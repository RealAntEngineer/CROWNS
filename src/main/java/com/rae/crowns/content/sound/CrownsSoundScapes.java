package com.rae.crowns.content.sound;


import com.rae.crowns.init.client.SoundInit;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;

public class CrownsSoundScapes {

    static final int MAX_AMBIENT_SOURCE_DISTANCE = 16;
    static final int UPDATE_INTERVAL = 5;
    static final int SOUND_VOLUME_ARG_MAX = 15;
    private static final Map<AmbienceGroup, Map<PitchGroup, Set<BlockPos>>> counter = new IdentityHashMap<>();
    private static final Map<Pair<AmbienceGroup, PitchGroup>, SoundScape> activeSounds = new HashMap<>();

    private static SoundScape kinetic(float pitch, AmbienceGroup group) {
        return new SoundScape(pitch, group).continuous(SoundInit.TURBINE_SOUND.get(), 2f, 1);
    }

    public static void play(@NotNull AmbienceGroup group, @NotNull BlockPos pos, float pitch) {
        if (!AllConfigs.client().enableAmbientSounds.get())
            return;
        if (!outOfRange(pos))
            addSound(group, pos, pitch);
    }

    protected static boolean outOfRange(@NotNull BlockPos pos) {
        return !getCameraPos().closerThan(pos, MAX_AMBIENT_SOURCE_DISTANCE);
    }

    private static void addSound(@NotNull AmbienceGroup group, BlockPos pos, float pitch) {
        PitchGroup groupFromPitch = getGroupFromPitch(pitch);
        Set<BlockPos> set = counter.computeIfAbsent(group, ag -> new IdentityHashMap<>())
                .computeIfAbsent(groupFromPitch, pg -> new HashSet<>());
        set.add(pos);

        Pair<AmbienceGroup, PitchGroup> pair = Pair.of(group, groupFromPitch);
        activeSounds.computeIfAbsent(pair, $ -> {
            SoundScape soundScape = group.instantiate(pitch);
            soundScape.play();
            return soundScape;
        });
    }

    protected static @NotNull BlockPos getCameraPos() {
        Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
        if (renderViewEntity == null)
            return BlockPos.ZERO;
        BlockPos playerLocation = renderViewEntity.blockPosition();
        return playerLocation;
    }

    public static @NotNull PitchGroup getGroupFromPitch(float pitch) {
        if (pitch < .70)
            return PitchGroup.VERY_LOW;
        if (pitch < .90)
            return PitchGroup.LOW;
        if (pitch < 1.10)
            return PitchGroup.NORMAL;
        if (pitch < 1.30)
            return PitchGroup.HIGH;
        return PitchGroup.VERY_HIGH;
    }

    public static void tick() {
        activeSounds.values()
                .forEach(SoundScape::tick);

        if (AnimationTickHolder.getTicks() % UPDATE_INTERVAL != 0)
            return;

        boolean disable = !AllConfigs.client().enableAmbientSounds.get();
        for (Iterator<Map.Entry<Pair<AmbienceGroup, PitchGroup>, SoundScape>> iterator = activeSounds.entrySet()
                .iterator(); iterator.hasNext(); ) {

            Map.Entry<Pair<AmbienceGroup, PitchGroup>, SoundScape> entry = iterator.next();
            Pair<AmbienceGroup, PitchGroup> key = entry.getKey();
            SoundScape value = entry.getValue();

            if (disable || getSoundCount(key.getFirst(), key.getSecond()) == 0) {
                value.remove();
                iterator.remove();
            }
        }

        counter.values()
                .forEach(m -> m.values()
                        .forEach(Set::clear));
    }

    public static int getSoundCount(AmbienceGroup group, PitchGroup pitchGroup) {
        return getAllLocations(group, pitchGroup).size();
    }

    public static Set<BlockPos> getAllLocations(AmbienceGroup group, PitchGroup pitchGroup) {
        return counter.getOrDefault(group, Collections.emptyMap())
                .getOrDefault(pitchGroup, Collections.emptySet());
    }

    public static void invalidateAll() {
        counter.clear();
        activeSounds.forEach(($, sound) -> sound.remove());
        activeSounds.clear();
    }

    public enum AmbienceGroup {
        TURBINE(CrownsSoundScapes::kinetic);

        private final BiFunction<Float, AmbienceGroup, SoundScape> factory;

        AmbienceGroup(BiFunction<Float, AmbienceGroup, SoundScape> factory) {
            this.factory = factory;
        }

        public SoundScape instantiate(float pitch) {
            return factory.apply(pitch, this);
        }

    }

    enum PitchGroup {
        VERY_LOW, LOW, NORMAL, HIGH, VERY_HIGH
    }
}
