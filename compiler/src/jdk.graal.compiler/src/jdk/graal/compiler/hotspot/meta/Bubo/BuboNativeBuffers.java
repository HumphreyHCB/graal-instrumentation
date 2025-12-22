package jdk.graal.compiler.hotspot.meta.Bubo;

import jdk.graal.compiler.serviceprovider.GlobalAtomicLong;
import jdk.internal.misc.Unsafe;

/**
 * Native buffers for Bubo instrumentation.
 * 
 * Base addresses are stored in GlobalAtomicLong so they are visible across
 * isolates.
 */
public final class BuboNativeBuffers {
    private static volatile Unsafe U;

    // Base addresses (shareable across isolates)
    private static final GlobalAtomicLong TIME_ADDR = new GlobalAtomicLong("Bubo.TIME_PTR", 0L);
    private static final GlobalAtomicLong ACT_ADDR  = new GlobalAtomicLong("Bubo.ACT_PTR", 0L);
    private static final GlobalAtomicLong CYC_ADDR  = new GlobalAtomicLong("Bubo.CYC_PTR", 0L);
    private static final GlobalAtomicLong CALL_ADDR = new GlobalAtomicLong("Bubo.CALL_PTR", 0L);

    /**
     * Total number of 8-byte cells we allocate off-heap.
     * This stays the same as your original code.
     */
    private static final int CAPACITY = 200_000;

    /**
     * How many loop counters we reserve per compilation unit.
     *   maxCompilations * MAX_LOOPS_PER_COMP <= CAPACITY
     */
    public static final int MAX_LOOPS_PER_COMP = 32;

    private static final long BYTES_PER_ELEM = Long.BYTES;

    public static void ensureInitialized() {
        if (TIME_ADDR.get() != 0L) {
            return;
        }
        synchronized (BuboNativeBuffers.class) {
            if (TIME_ADDR.get() != 0L) {
                return;
            }
            Unsafe uu = u();

            long timePtr = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            long actPtr  = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            long cycPtr  = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            long callPtr = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);

            // zero them
            uu.setMemory(timePtr, CAPACITY * BYTES_PER_ELEM, (byte) 0);
            uu.setMemory(actPtr,  CAPACITY * BYTES_PER_ELEM, (byte) 0);
            uu.setMemory(cycPtr,  CAPACITY * BYTES_PER_ELEM, (byte) 0);
            uu.setMemory(callPtr, CAPACITY * BYTES_PER_ELEM, (byte) 0);

            TIME_ADDR.set(timePtr);
            ACT_ADDR.set(actPtr);
            CYC_ADDR.set(cycPtr);
            CALL_ADDR.set(callPtr);
        }
    }

    public static long timePtr() {
        ensureInitialized();
        return TIME_ADDR.get();
    }

    public static long activationPtr() {
        ensureInitialized();
        return ACT_ADDR.get();
    }

    public static long cyclesPtr() {
        ensureInitialized();
        return CYC_ADDR.get();
    }

    public static long callSitePtr() {
        ensureInitialized();
        return CALL_ADDR.get();
    }

    /**
     * Compute the flat index into the buffer for a given (compilationId, loopId).
     * Layout is:
     *   comp0: loop0 .. loopN-1
     *   comp1: loop0 .. loopN-1
     *   ...
     */
    private static long flatIndex(int compilationId, int loopId) {
        // clamp loopId just in case
        int safeLoopId = Math.max(0, Math.min(loopId, MAX_LOOPS_PER_COMP - 1));
        return ((long) compilationId) * MAX_LOOPS_PER_COMP + (long) safeLoopId;
    }

    /**
     * Same pattern for time buffer, if you want per-loop time as well.
     */
    public static long timeLoopAddr(int compilationId, int loopId) {
        ensureInitialized();
        long idx = flatIndex(compilationId, loopId);
        if (idx >= CAPACITY) {
            return TIME_ADDR.get() + (((long) compilationId) << 3);
        }
        return TIME_ADDR.get() + (idx << 3);
    }

    public static long activationLoopAddr(int compilationId, int loopId) {
        ensureInitialized();
        long idx = flatIndex(compilationId, loopId);
        if (idx >= CAPACITY) {
            return ACT_ADDR.get() + (((long) compilationId) << 3);
        }
        return ACT_ADDR.get() + (idx << 3);
    }


    /**
     * Same for cycles.
     */
    public static long cyclesLoopAddr(int compilationId, int loopId) {
        ensureInitialized();
        long idx = flatIndex(compilationId, loopId);
        if (idx >= CAPACITY) {
            return CYC_ADDR.get() + (((long) compilationId) << 3);
        }
        return CYC_ADDR.get() + (idx << 3);
    }

    public static void freeAll() {
        synchronized (BuboNativeBuffers.class) {
            Unsafe uu = u();

            long t = TIME_ADDR.get();
            long a = ACT_ADDR.get();
            long c = CYC_ADDR.get();
            long s = CALL_ADDR.get();

            if (t != 0L) {
                uu.freeMemory(t);
                TIME_ADDR.set(0L);
            }
            if (a != 0L) {
                uu.freeMemory(a);
                ACT_ADDR.set(0L);
            }
            if (c != 0L) {
                uu.freeMemory(c);
                CYC_ADDR.set(0L);
            }
            if (s != 0L) {
                uu.freeMemory(s);
                CALL_ADDR.set(0L);
            }
        }
    }

    private static Unsafe u() {
        Unsafe x = U;
        if (x == null) {
            x = Unsafe.getUnsafe();
            U = x;
        }
        return x;
    }

    public static int capacity() {
        return CAPACITY;
    }

    public static long readActivationAt(int idx) {
        ensureInitialized();
        return u().getLong(ACT_ADDR.get() + (((long) idx) << 3));
    }

    public static long readTimeAt(int idx) {
        ensureInitialized();
        return u().getLong(TIME_ADDR.get() + (((long) idx) << 3));
    }

    public static long readCyclesAt(int idx) {
        ensureInitialized();
        return u().getLong(CYC_ADDR.get() + (((long) idx) << 3));
    }

    public static long readCallSiteAt(int idx) {
        ensureInitialized();
        return u().getLong(CALL_ADDR.get() + (((long) idx) << 3));
    }

    public static int countNonZeroTime(int upToExclusive) {
        ensureInitialized();
        int n = Math.min(upToExclusive, CAPACITY);
        int count = 0;
        long base = TIME_ADDR.get();
        Unsafe uu = u();
        for (int i = 0; i < n; i++) {
            if (uu.getLong(base + (((long) i) << 3)) != 0L) {
                count++;
            }
        }
        return count;
    }

    private BuboNativeBuffers() {
    }
}
