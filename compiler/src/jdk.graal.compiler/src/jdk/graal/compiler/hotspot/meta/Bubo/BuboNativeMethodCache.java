package jdk.graal.compiler.hotspot.meta.Bubo;

import jdk.graal.compiler.serviceprovider.GlobalAtomicLong;
import jdk.internal.misc.Unsafe;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Off-heap, cross-isolate method cache using GlobalAtomicLong for persistence.
 * - Fixed-size zero-terminated UTF-8 strings in a ring buffer.
 * - Cross-isolate atomic write index via GlobalAtomicLong CAS.
 * - No shutdown hook here (lifecycle managed elsewhere).
 */
public final class BuboNativeMethodCache {

    // ---- Configuration ----
    private static final int CAPACITY    = 10_000; // number of entries
    private static final int ENTRY_BYTES = 256;    // bytes per entry (incl. NUL)

    // ---- Cross-isolate globals ----
    /** Base address of the contiguous native buffer (CAPACITY * ENTRY_BYTES). */
    private static final GlobalAtomicLong METHOD_BUF_ADDR =
            new GlobalAtomicLong("Bubo.METHOD_BUF_BASE", 0L);

    /** Monotonic global write index (not a pointer). */
    private static final GlobalAtomicLong METHOD_IDX =
            new GlobalAtomicLong("Bubo.METHOD_IDX", 0L);

    /** Init guard: 0 = uninitialized, 1 = initializing, 2 = initialized. */
    private static final GlobalAtomicLong INIT_STATE =
            new GlobalAtomicLong("Bubo.METHOD_INIT_STATE", 0L);

    private static volatile Unsafe U;


    /** Ensure the native buffer exists (exactly once across all isolates). */
    public static void ensureInitialized() {
        long s = INIT_STATE.get();
        if (s == 2L && METHOD_BUF_ADDR.get() != 0L) return;

        // Try to win initialization
        if (s == 0L && INIT_STATE.compareAndSet(0L, 1L)) {
            Unsafe u = unsafe();
            long total = (long) CAPACITY * ENTRY_BYTES;
            long buf   = u.allocateMemory(total);
            u.setMemory(buf, total, (byte) 0);

            METHOD_BUF_ADDR.set(buf);
            // publish
            INIT_STATE.set(2L);
            u.storeFence();
            return;
        }

        // Wait until publisher sets the base address
        while (METHOD_BUF_ADDR.get() == 0L || INIT_STATE.get() != 2L) {
            Thread.onSpinWait();
        }
    }

    /** Append a method string; atomic and cross-isolate safe. */
    public static void add(String method) {
        ensureInitialized();

        // Global atomic fetch-add via CAS on GlobalAtomicLong
        long idx;
        do {
            idx = METHOD_IDX.get();
        } while (!METHOD_IDX.compareAndSet(idx, idx + 1));

        int slot = (int) (idx % CAPACITY);
        long entry = METHOD_BUF_ADDR.get() + (long) slot * ENTRY_BYTES;

        Unsafe u = unsafe();
        byte[] src = (method == null) ? new byte[0] : method.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(src.length, ENTRY_BYTES - 1);

        u.setMemory(entry, ENTRY_BYTES, (byte) 0);
        copyToNative(u, src, 0, len, entry);
        u.putByte(entry + len, (byte) 0);
        u.storeFence();
    }

    /**
     * Snapshot into a map like the original:
     * key = parsed Integer from "xxx-<id>", value = method name (with demo "-Re-Comp" tweak).
     * Iterates the last CAPACITY writes in chronological order.
     */
    public static HashMap<Integer, String> getBuffer() {
        ensureInitialized();
        Unsafe u = unsafe();
        u.loadFence();

        long written = METHOD_IDX.get();
        long base   = METHOD_BUF_ADDR.get();
        long start  = Math.max(0L, written - CAPACITY);
        HashMap<Integer, String> out = new HashMap<>((int) Math.min(written, CAPACITY) * 2);

        for (long i = start; i < written; i++) {
            int slot = (int) (i % CAPACITY);
            long entry = base + (long) slot * ENTRY_BYTES;
            String s = readCString(u, entry, ENTRY_BYTES);
            if (s == null || s.isEmpty()) continue;

            String[] parts = s.split(" ");
            if (parts.length < 2) continue;

            // Preserve your demo behavior: mark duplicates as re-compilations.
            if (out.containsValue(parts[1])) {
                parts[1] = parts[1] + "-Re-Comp";
            }

            try {
                String[] left = parts[0].split("-");
                if (left.length >= 2) {
                    int id = Integer.parseInt(left[1]);
                    out.put(id, parts[1]);
                }
            } catch (NumberFormatException ignore) {
                // skip malformed
            }
        }
        return out;
    }

    /** Optional manual teardown if you manage lifecycle elsewhere. */
    public static void freeAll() {
        Unsafe u = unsafe();
        long b = METHOD_BUF_ADDR.get();
        if (b != 0L) {
            u.freeMemory(b);
            METHOD_BUF_ADDR.set(0L);
        }
        INIT_STATE.set(0L);
        METHOD_IDX.set(0L);
    }

    // ---- Helpers ----

    private static Unsafe unsafe() {
        Unsafe uu = U;
        if (uu == null) {
            uu = Unsafe.getUnsafe(); // valid in JDK/Graal internals
            U = uu;
        }
        return uu;
    }

    private static void copyToNative(Unsafe u, byte[] src, int off, int len, long dst) {
        int i = 0, step = Long.BYTES;
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
            if (u.getByte(addr + len) == 0) break;
        }
        if (len == 0) return "";
        byte[] out = new byte[len];

        int i = 0, step = Long.BYTES;
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

    private BuboNativeMethodCache() {}
}
