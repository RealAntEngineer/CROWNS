package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.temperature.TemperatureSolver;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public class PhysicThread extends Thread {
    private static   PhysicThread      INSTANCE;
    /**
     * Capacity: at 20 TPS that's 1 200 ticks/minute — give a comfortable margin.
     * Adjust if your TPS is much higher.
     */
    public final Stats stats = new Stats(4_000);
    private final    TemperatureSolver tempSolver = new TemperatureSolver();
    private final    long              intervalNs;
    private volatile boolean           running    = true;
    private          int               tickCounter;

    private volatile boolean paused        = false;
    private volatile boolean resetTickClock = false;
    private final    Object  pauseLock      = new Object();

    public PhysicThread(double ticksPerSecond) {
        this.intervalNs = (long) (1_000_000_000D / ticksPerSecond);
        setName("Physics-Thread");
        setDaemon(true);
    }

    public void pause() {
        synchronized (pauseLock) {
            paused = true;
        }
    }

    public void unpause() {
        synchronized (pauseLock) {
            if (!paused) return;
            paused = false;
            resetTickClock = true; // avoid a catch-up burst after a long pause
            pauseLock.notifyAll();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public static void launchPhysicThread(double tps) {
        INSTANCE = new PhysicThread(tps);
        INSTANCE.start();
    }

    public static PhysicThread getInstance() {
        return INSTANCE;
    }

    public static void shutdown() {
        INSTANCE.running = false;
        INSTANCE.interrupt();
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run() {
        long nextTickTime = System.nanoTime();
        tempSolver.setTimeStep(TemperatureSolver.DT);

        while (running) {
            synchronized (pauseLock) {
                while (paused && running) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException ignored) {
                        if (!running) return;
                    }
                }
                if (resetTickClock) {
                    nextTickTime = System.nanoTime();
                    resetTickClock = false;
                }
            }

            long now = System.nanoTime();

            // If we're behind, catch up as fast as possible — but bail out
            // immediately if a pause comes in mid-burst.
            while (now >= nextTickTime && !paused) {
                long tickStart = System.nanoTime();

                for (ServerLevel serverLevel : PhysicsSaveManager.getServers()) {
                    tick(serverLevel);
                }

                long tickDuration = System.nanoTime() - tickStart;
                stats.record(tickDuration);

                nextTickTime += intervalNs;
                tempSolver.setTimeStep((float) (tickDuration / 1_000_000_000D));

                now = System.nanoTime();
            }

            long sleepTime = nextTickTime - now;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
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
        data.updateChangedBlocks(serverLevel, tempSolver);
        //this is too long... do the gathering of section to tick every few iteration (10 ticks ?)
        LongSet loadedSections      = data.getLoadedSections(); // LongSet view of keys
        LongSet nearDynamicSections = data.getNearDynamic();
        LongSet toTick              = new LongOpenHashSet();

        // Compute intersection efficiently
        for (long packed : nearDynamicSections) {
            if (loadedSections.contains(packed)) {
                //verify data
                if (data.checkValidity(packed) && data.needTicking(packed)) {//) {
                    toTick.add(packed);
                    data.addToTicked(packed);
                }
            }
        }

        tempSolver.tick(toTick, data);
        //RANSTicker.tick(toTick, data);

        if (tickCounter % 20 == 0) {
            PhysicsSaveManager.sendUpdate(serverLevel);
        }
        tickCounter++;
    }

    public boolean isRunning() {
        return running;
    }

    public static class Stats {
        private static final long WINDOW_NS = 60_000_000_000L; // 1 minute

        /**
         * Ring-buffer entries: nanosecond timestamp of end of tick
         */
        private final long[] timestamps;
        /**
         * Ring-buffer entries: duration of that tick in nanoseconds
         */
        private final long[] durations;
        private final int    capacity;
        private       int    head = 0;   // next write position
        private       int    size = 0;   // valid entries

        public Stats(int capacity) {
            this.capacity = capacity;
            this.timestamps = new long[capacity];
            this.durations = new long[capacity];
        }

        /**
         * Record one tick whose wall-clock duration is {@code durationNs}.
         */
        public synchronized void record(long durationNs) {
            long now = System.nanoTime();
            timestamps[head] = now;
            durations[head] = durationNs;
            head = (head + 1) % capacity;
            if (size < capacity) size++;
        }

        /**
         * Human-readable summary, e.g. for periodic logging.
         */
        public String summary() {
            double[] s = snapshot();
            if (s == null) return "Stats: no data yet";
            return String.format(
                    "Tick stats (1 min window) — mean: %.2f ms | min: %.2f ms | max: %.2f ms | median: %.2f ms | stddev: %.2f ms",
                    s[0], s[1], s[2], s[3], s[4]);
        }

        /**
         * Returns [mean, min, max, median, stddev] in milliseconds over the last 60 s,
         * or null if no samples are available yet.
         */
        public synchronized double[] snapshot() {
            long now    = System.nanoTime();
            long cutoff = now - WINDOW_NS;

            // Collect valid samples from newest to oldest
            long[] window = new long[size];
            int count = 0;

            for (int i = 0; i < size; i++) {
                int idx = ((head - 1 - i) % capacity + capacity) % capacity;
                if (timestamps[idx] < cutoff) break;
                window[count++] = durations[idx];
            }

            if (count == 0) return null;

            // Mean + min/max
            long sum = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (int i = 0; i < count; i++) {
                long d = window[i];
                sum += d;
                if (d < min) min = d;
                if (d > max) max = d;
            }
            double mean = (double) sum / count;

            // Standard deviation
            double variance = 0;
            for (int i = 0; i < count; i++) {
                double diff = window[i] - mean;
                variance += diff * diff;
            }
            double stddev = Math.sqrt(variance / count);

            // Median (sort a copy of the valid slice)
            long[] sorted = Arrays.copyOf(window, count);
            Arrays.sort(sorted);
            double median = (count % 2 == 0)
                    ? (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
                    : sorted[count / 2];

            double toMs = 1e-6;
            return new double[]{mean * toMs, min * toMs, max * toMs, median * toMs, stddev * toMs};
        }
    }
}