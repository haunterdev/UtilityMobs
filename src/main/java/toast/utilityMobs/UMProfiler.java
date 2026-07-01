package toast.utilityMobs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight, opt-in performance profiler for the golem AI / collision hot paths.
 *
 * Toggled by the {@code golems.performance_logging} config flag (mirrored into {@link #enabled}
 * by {@link Properties#load()}). When disabled, {@link #start()} returns 0 and every record call
 * short-circuits, so the instrumentation costs a single boolean check on the hot path.
 *
 * Timers track wall-clock nanos + call counts; counters track running totals (e.g. how many
 * entities were scanned, how many raytraces ran, how many collision pushes were capped). Stats
 * are dumped to the console and reset on a fixed interval by {@link TickHandler}.
 *
 * Methods are synchronized because {@code collideWithNearbyEntities} can run on both the integrated
 * server thread and the client thread; contention is negligible since profiling is a diagnostic mode.
 */
public final class UMProfiler {
    /// Master switch, set from the golems.performance_logging config flag.
    public static volatile boolean enabled = false;

    /// name -> [totalNanos, calls]
    private static final Map<String, long[]> TIMERS = new LinkedHashMap<String, long[]>();
    /// name -> [runningTotal, samples]
    private static final Map<String, long[]> COUNTERS = new LinkedHashMap<String, long[]>();

    private UMProfiler() {}

    /// Returns a start timestamp, or 0 when profiling is off (so end() becomes a no-op).
    public static long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    /// Records elapsed time for a labelled timer. No-op if profiling is off or start was 0.
    public static synchronized void end(String label, long start) {
        if (!enabled || start == 0L)
            return;
        long dt = System.nanoTime() - start;
        long[] d = TIMERS.get(label);
        if (d == null) {
            d = new long[2];
            TIMERS.put(label, d);
        }
        d[0] += dt;
        d[1]++;
    }

    /// Adds to a labelled counter (e.g. entities scanned, raytraces, pushes skipped).
    public static synchronized void count(String label, long amount) {
        if (!enabled)
            return;
        long[] d = COUNTERS.get(label);
        if (d == null) {
            d = new long[2];
            COUNTERS.put(label, d);
        }
        d[0] += amount;
        d[1]++;
    }

    /// Prints accumulated stats and resets all tallies. Called on an interval by the tick handler.
    public static synchronized void dump(int overTicks) {
        if (!enabled)
            return;
        _UtilityMobs.console("==== UM golem profiler (last ", Integer.toString(overTicks), " ticks) ====");
        if (TIMERS.isEmpty() && COUNTERS.isEmpty()) {
            _UtilityMobs.console("  (no samples - no golems ticking?)");
        }
        for (Map.Entry<String, long[]> e : TIMERS.entrySet()) {
            long[] d = e.getValue();
            double totalMs = d[0] / 1_000_000.0;
            double avgUs = d[1] == 0 ? 0.0 : (d[0] / 1_000.0) / d[1];
            double msPerTick = d[0] / 1_000_000.0 / Math.max(1, overTicks);
            _UtilityMobs.console(String.format(
                    "  [time] %-22s total=%8.3fms  calls=%-8d avg=%7.2fus  %6.3fms/tick",
                    e.getKey(), totalMs, d[1], avgUs, msPerTick));
        }
        for (Map.Entry<String, long[]> e : COUNTERS.entrySet()) {
            long[] d = e.getValue();
            double perTick = (double) d[0] / Math.max(1, overTicks);
            _UtilityMobs.console(String.format(
                    "  [count] %-21s total=%-10d  %8.1f/tick", e.getKey(), d[0], perTick));
        }
        TIMERS.clear();
        COUNTERS.clear();
    }
}
