package jdk.graal.compiler.hotspot.meta.Bubo;

import jdk.graal.compiler.serviceprovider.GlobalAtomicLong;
import jdk.internal.misc.Unsafe;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class BuboNativeLoopNestingCache {

    // Max number of compilation units we support.
    // Each compId gets one fixed slot [0..CAPACITY-1].
    private static final int CAPACITY    = 20_000;
    private static final int ENTRY_BYTES = 128; // bytes per compId encoding

    private static final GlobalAtomicLong BUF_ADDR =
            new GlobalAtomicLong("Bubo.LOOP_NEST_BUF_BASE", 0L);

    // 0 = uninitialized, 1 = initializing, 2 = initialized
    private static final GlobalAtomicLong INIT_STATE =
            new GlobalAtomicLong("Bubo.LOOP_NEST_INIT", 0L);

    private static volatile Unsafe U;

    private BuboNativeLoopNestingCache() {
    }

    // -------------------------------------------------------------------------
    // public API
    // -------------------------------------------------------------------------

    /**
     * Store the loop nesting encoding string for a given compilation id.
     *
     * The encoding is something like:
     *   "1:4,2:4,4:3"
     *
     * where each "child:parent" pair is separated by commas and
     * parent == -1 means "no parent / top-level loop".
     *
     * NOTE: The compId is NOT encoded in the string, because the compId
     * is implied by the slot we write to.
     */
    public static void putEncoding(int compId, String encoding) {
        ensureInitialized();
        if (compId < 0 || compId >= CAPACITY) {
            // out of range, ignore or log as needed
            return;
        }

        Unsafe u = unsafe();
        long base = BUF_ADDR.get();
        long entry = base + (long) compId * ENTRY_BYTES;

        byte[] bytes = encoding.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, ENTRY_BYTES - 1); // reserve 1 byte for NUL

        // Clear slot, copy bytes, and add NUL terminator
        u.setMemory(entry, ENTRY_BYTES, (byte) 0);
        copyToNative(u, bytes, 0, len, entry);
        u.putByte(entry + len, (byte) 0);
        u.storeFence();
    }

    /**
     * Retrieve the encoding string for a given compId, or null if none was stored.
     */
    public static String getEncoding(int compId) {
        ensureInitialized();
        if (compId < 0 || compId >= CAPACITY) {
            return null;
        }

        Unsafe u = unsafe();
        long base = BUF_ADDR.get();
        long entry = base + (long) compId * ENTRY_BYTES;

        String s = readCString(u, entry, ENTRY_BYTES);
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }

    /**
     * Optional helper: snapshot all encodings into a Map:
     *   compId -> encoding
     * Only slots with non-empty encodings are included.
     */
    public static Map<Integer, String> snapshot() {
        ensureInitialized();
        Unsafe u = unsafe();
        u.loadFence();

        long base = BUF_ADDR.get();
        Map<Integer, String> out = new HashMap<>();

        for (int compId = 0; compId < CAPACITY; compId++) {
            long entry = base + (long) compId * ENTRY_BYTES;
            String s = readCString(u, entry, ENTRY_BYTES);
            if (s == null || s.isEmpty()) {
                continue;
            }
            out.put(compId, s);
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
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------

    private static void ensureInitialized() {
        long s = INIT_STATE.get();
        if (s == 2L && BUF_ADDR.get() != 0L) {
            return;
        }

        if (s == 0L && INIT_STATE.compareAndSet(0L, 1L)) {
            Unsafe u = unsafe();
            long total = (long) CAPACITY * ENTRY_BYTES;
            long buf = u.allocateMemory(total);
            u.setMemory(buf, total, (byte) 0);

            BUF_ADDR.set(buf);
            INIT_STATE.set(2L);
            u.storeFence();
            return;
        }

        // Wait for other thread to finish initialization
        while (BUF_ADDR.get() == 0L || INIT_STATE.get() != 2L) {
            Thread.onSpinWait();
        }
    }

    private static Unsafe unsafe() {
        Unsafe uu = U;
        if (uu == null) {
            uu = Unsafe.getUnsafe();
            U = uu;
        }
        return uu;
    }

    // -------------------------------------------------------------------------
    // native string helpers
    // -------------------------------------------------------------------------

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
