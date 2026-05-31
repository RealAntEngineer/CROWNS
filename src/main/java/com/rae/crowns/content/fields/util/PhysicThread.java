package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.temperature.MatrixTemperatureTicker;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;

public class PhysicThread extends Thread {

    private static   PhysicThread INSTANCE;
    private final    long         intervalNs;
    private volatile boolean      running = true;
    private          int          tickCounter;

    public PhysicThread(double ticksPerSecond) {
        this.intervalNs = (long) (1_000_000_000D / ticksPerSecond);
        setName("Physics-Thread");
        setDaemon(true);
    }

    public static void launchPhysicThread(double tps) {
        INSTANCE = new PhysicThread(tps);
        INSTANCE.start();
    }

    public static void shutdown() {
        INSTANCE.running = false;
        INSTANCE.interrupt();
    }

    @Override
    public void run() {
        long nextTickTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();

            if (now >= nextTickTime) {
                for (ServerLevel serverLevel : PhysicsSaveManager.getServers()) {
                    tick(serverLevel);
                }

                nextTickTime += intervalNs;

                // Catch up if we're lagging behind
                if (now > nextTickTime) {
                    nextTickTime = now + intervalNs;
                }
            } else {
                long sleepTime = nextTickTime - now;

                // Sleep with nanosecond precision
                try {
                    Thread.sleep(
                            sleepTime / 1_000_000,
                            (int) (sleepTime % 1_000_000)
                    );
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void tick(ServerLevel serverLevel) {

        PhysicsWorldData data = PhysicsSaveManager.get(serverLevel);
        if (data == null) return;
        data.setCurrentTime((int) serverLevel.getGameTime());
        data.initialise(serverLevel);
        data.updateChangedBlocks(serverLevel);
        //this is too long... do the gathering of section to tick every few iteration (10 ticks ?)
        LongSet loadedSections      = data.getLoadedSections(); // LongSet view of keys
        LongSet nearDynamicSections = data.getNearDynamic();
        LongSet toTick              = new LongOpenHashSet();

        // Compute intersection efficiently
        for (long packed : nearDynamicSections) {
            if (loadedSections.contains(packed)) {
                //verify data
                if (data.checkValidity(packed)) {//&& data.isDirty(packed)) {
                    toTick.add(packed);
                }
            }
        }

        MatrixTemperatureTicker.tick(toTick, data);
        //RANSTicker.tick(toTick, data);


        if (tickCounter % (20) == 0) {
            PhysicsSaveManager.sendUpdate(serverLevel);
        }
        SteamFlowManager.tick(serverLevel);
        tickCounter++;
    }

}