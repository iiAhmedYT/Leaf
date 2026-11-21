package org.dreeam.leaf.debug;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Debug-only detector for off-thread writes to a chunk's block entity and ticker maps.
 * <p>
 * Enabled with {@code -Dleaf.debug.chunkmapthreads=true}. When enabled, chunk block entity map writes are checked
 * against the owning tick thread. A mismatch is logged with the thread name, chunk position, dimension and a stack
 * trace. This class only reports; it never changes behavior.
 * <p>
 * Logging is rate-limited to one message per second and at most {@link #MAX_LOGS} messages in total, so a broken
 * plugin cannot flood the log.
 */
public final class ChunkBlockEntityThreadDebug {

    public static final boolean ENABLED = Boolean.getBoolean("leaf.debug.chunkmapthreads");

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long LOG_INTERVAL_NANOS = 1_000_000_000L;
    private static final int MAX_LOGS = 200;

    private static long lastLogNanos = System.nanoTime() - LOG_INTERVAL_NANOS;
    private static int logCount;

    private ChunkBlockEntityThreadDebug() {
    }

    /**
     * Checks that the current thread may write to the block entity maps of the given chunk.
     *
     * @param level the level owning the chunk
     * @param pos the chunk position
     * @param operation the name of the mutating operation, used in the log message
     */
    public static void check(final Level level, final ChunkPos pos, final String operation) {
        if (!ENABLED) {
            return;
        }
        if (TickThread.isTickThreadFor(level, pos)) {
            return;
        }
        if (!shouldLog()) {
            return;
        }
        LOGGER.error(
            "Off-thread chunk block entity map write detected: operation={}, thread={}, chunk={}, dimension={}",
            operation,
            Thread.currentThread().getName(),
            pos,
            level.dimension().identifier(),
            new Throwable("Off-thread chunk block entity map write")
        );
    }

    private static synchronized boolean shouldLog() {
        if (logCount >= MAX_LOGS) {
            return false;
        }
        final long now = System.nanoTime();
        if (now - lastLogNanos < LOG_INTERVAL_NANOS) {
            return false;
        }
        lastLogNanos = now;
        logCount++;
        return true;
    }
}
