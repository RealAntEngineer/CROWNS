package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.temperature.TemperatureSolver;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;

public class PhysicThread extends Thread {

    private static   PhysicThread      INSTANCE;
    private final    TemperatureSolver tempSolver = new TemperatureSolver();
    private final    long              intervalNs;
    private volatile boolean           running    = true;
    private          int               tickCounter;

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

        tempSolver.setTimeStep(TemperatureSolver.DT);

        while (running) {
            long now = System.nanoTime();

            // If we're behind, catch up as fast as possible
            while (now >= nextTickTime) {
                for (ServerLevel serverLevel : PhysicsSaveManager.getServers()) {
                    tick(serverLevel);
                }
                nextTickTime += intervalNs;
                tempSolver.setTimeStep((float) ((System.nanoTime() - now)/ 1_000_000_000D));
                //System.out.println("Ticking time" + (System.nanoTime() - now) / 1e6 + "ms");
                // Update now so we don't spin unnecessarily
                now = System.nanoTime();
            }

            // Sleep until next tick (if ahead)
            long sleepTime = nextTickTime - now;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(
                            sleepTime / 1_000_000,
                            (int) (sleepTime % 1_000_000)
                    );
                } catch (InterruptedException ignored) {}
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
                    data.addToTicked(packed);
                }
            }
        }

        tempSolver.tick(toTick, data);
        //RANSTicker.tick(toTick, data);

        if (tickCounter % (20) == 0) {
            PhysicsSaveManager.sendUpdate(serverLevel);
        }
        tickCounter++;
    }
}