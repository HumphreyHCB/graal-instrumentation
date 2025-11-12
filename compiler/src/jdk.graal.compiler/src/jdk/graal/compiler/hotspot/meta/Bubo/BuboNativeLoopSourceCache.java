package jdk.graal.compiler.hotspot.meta.Bubo;

import jdk.graal.compiler.serviceprovider.GlobalAtomicLong;
import jdk.internal.misc.Unsafe;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Off-heap, cross-isolate loop-source cache.
 *
 * Each entry is:
 *   "<compId>:<loopId> <source>"
 * stored as zero-terminated UTF-8 in a fixed-size ring buffer.
 *
 * Layout is the same style as BuboNativeMethodCache so we get the same
 * cross-isolate behavior.
 */
public final class BuboNativeLoopSourceCache {

    // tune these if you want
    private static final int CAPACITY    = 20_000; // number of entries
    private static final int ENTRY_BYTES = 256;    // bytes per entry (incl. NUL)

    // global base pointer
    private static final GlobalAtomicLong BUF_ADDR =
            new GlobalAtomicLong("Bubo.LOOP_SRC_BUF_BASE", 0L);

    // global monotonic write index
    private static final GlobalAtomicLong WRITE_IDX =
            new GlobalAtomicLong("Bubo.LOOP_SRC_IDX", 0L);

    // 0 = uninitialized, 1 = initializing, 2 = initialized
    private static final GlobalAtomicLong INIT_STATE =
            new GlobalAtomicLong("Bubo.LOOP_SRC_INIT", 0L);

    private static volatile Unsafe U;

    private BuboNativeLoopSourceCache() {}

    // -------------------------------------------------------------------------
    // public API
    // -------------------------------------------------------------------------

    /**
     * Store source for (compId, loopId).
     * Will overwrite older entries in ring fashion.
     */
    public static void add(int compId, int loopId, String source) {
        ensureInitialized();

        // bump global index atomically
        long idx;
        do {
            idx = WRITE_IDX.get();
        } while (!WRITE_IDX.compareAndSet(idx, idx + 1));

        int slot = (int) (idx % CAPACITY);
        long entry = BUF_ADDR.get() + (long) slot * ENTRY_BYTES;

        Unsafe u = unsafe();

        // format: "<compId>:<loopId> <source>"
        String payload;
        if (source == null) {
            payload = compId + ":" + loopId + " ";
        } else {
            payload = compId + ":" + loopId + " " + source;
        }

        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, ENTRY_BYTES - 1);

        // clear entry then copy
        u.setMemory(entry, ENTRY_BYTES, (byte) 0);
        copyToNative(u, bytes, 0, len, entry);
        u.putByte(entry + len, (byte) 0);
        u.storeFence();
    }

    /**
     * Read the buffer back into a nested map:
     *   compId -> (loopId -> source)
     *
     * Last writer wins.
     */
    public static Map<Integer, Map<Integer, String>> snapshot() {
        ensureInitialized();
        Unsafe u = unsafe();
        u.loadFence();

        long written = WRITE_IDX.get();
        long base    = BUF_ADDR.get();
        long start   = Math.max(0L, written - CAPACITY);

        Map<Integer, Map<Integer, String>> out = new HashMap<>();

        for (long i = start; i < written; i++) {
            int slot = (int) (i % CAPACITY);
            long entry = base + (long) slot * ENTRY_BYTES;
            String s = readCString(u, entry, ENTRY_BYTES);
            if (s == null || s.isEmpty()) {
                continue;
            }

            // s = "<compId>:<loopId> <source...>"
            int spacePos = s.indexOf(' ');
            if (spacePos <= 0) {
                continue;
            }
            String key = s.substring(0, spacePos);
            String src = s.substring(spacePos + 1);

            int colonPos = key.indexOf(':');
            if (colonPos <= 0) {
                continue;
            }

            try {
                int compId = Integer.parseInt(key.substring(0, colonPos));
                int loopId = Integer.parseInt(key.substring(colonPos + 1));

                out.computeIfAbsent(compId, k -> new HashMap<>())
                   .put(loopId, src);
            } catch (NumberFormatException ignore) {
                // malformed, skip
            }
        }

        return out;
    }

    /**
     * Optional cleanup.
     */
    public static void freeAll() {
        Unsafe u = unsafe();
        long b = BUF_ADDR.get();
        if (b != 0L) {
            u.freeMemory(b);
            BUF_ADDR.set(0L);
        }
        INIT_STATE.set(0L);
        WRITE_IDX.set(0L);
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------

    private static void ensureInitialized() {
        long s = INIT_STATE.get();
        if (s == 2L && BUF_ADDR.get() != 0L) {
            return;
        }

        // try to become initializer
        if (s == 0L && INIT_STATE.compareAndSet(0L, 1L)) {
            Unsafe u = unsafe();
            long total = (long) CAPACITY * ENTRY_BYTES;
            long buf   = u.allocateMemory(total);
            u.setMemory(buf, total, (byte) 0);

            BUF_ADDR.set(buf);
            INIT_STATE.set(2L);
            u.storeFence();
            return;
        }

        // otherwise spin until it's ready
        while (BUF_ADDR.get() == 0L || INIT_STATE.get() != 2L) {
            Thread.onSpinWait();
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static Unsafe unsafe() {
        Unsafe uu = U;
        if (uu == null) {
            uu = Unsafe.getUnsafe();
            U = uu;
        }
        return uu;
    }

    private static void copyToNative(Unsafe u, byte[] src, int off, int len, long dst) {
        int i = 0;
        int step = Long.BYTES;
        for (; i + step <= len; i += step) {
            long v = ((long) src[off + i] & 0xFF)
                   | (((long) src[off + i + 1] & 0xFF) << 8)
                   | (((long) src[off + i + 2] & 0xFF) << 16)
                   | (((long) src[off + i + 3] & 0xFF) << 24)
                   | (((long) src[off + i + 4] & 0xFF) << 32)
                   | (((long) src[off + i + 5] & 0xFF) << 40)
                   | (((long) src[off + i + 6] & 0xFF) << 48)
                   | (((long) src[off + i + 7] & 0xFF) << 56);
            u.putLong(dst + i, v);
        }
        for (; i < len; i++) {
            u.putByte(dst + i, src[off + i]);
        }
    }

    private static String readCString(Unsafe u, long addr, int max) {
        int len = 0;
        for (; len < max; len++) {
            if (u.getByte(addr + len) == 0) {
                break;
            }
        }
        if (len == 0) {
            return "";
        }
        byte[] out = new byte[len];

        int i = 0;
        int step = Long.BYTES;
        for (; i + step <= len; i += step) {
            long v = u.getLong(addr + i);
            out[i]     = (byte) (v);
            out[i + 1] = (byte) (v >>> 8);
            out[i + 2] = (byte) (v >>> 16);
            out[i + 3] = (byte) (v >>> 24);
            out[i + 4] = (byte) (v >>> 32);
            out[i + 5] = (byte) (v >>> 40);
            out[i + 6] = (byte) (v >>> 48);
            out[i + 7] = (byte) (v >>> 56);
        }
        for (; i < len; i++) {
            out[i] = u.getByte(addr + i);
        }
        return new String(out, StandardCharsets.UTF_8);
    }
}
