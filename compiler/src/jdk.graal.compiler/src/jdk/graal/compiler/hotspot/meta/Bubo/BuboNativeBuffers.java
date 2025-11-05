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
    private static final GlobalAtomicLong ACT_ADDR = new GlobalAtomicLong("Bubo.ACT_PTR", 0L);
    private static final GlobalAtomicLong CYC_ADDR = new GlobalAtomicLong("Bubo.CYC_PTR", 0L);
    private static final GlobalAtomicLong CALL_ADDR = new GlobalAtomicLong("Bubo.CALL_PTR", 0L);

    //private static final GlobalAtomicLong PMC_ADDR = new GlobalAtomicLong("Bubo.PMC_IDX_PTR", 0L);

    private static final int CAPACITY = 200_000;
    private static final long BYTES_PER_ELEM = Long.BYTES;

    public static void ensureInitialized() {
        if (TIME_ADDR.get() != 0L)
            return;
        synchronized (BuboNativeBuffers.class) {
            if (TIME_ADDR.get() != 0L)
                return;
            Unsafe uu = u();

            long timePtr = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            long actPtr = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            long cycPtr = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            long callPtr = uu.allocateMemory(CAPACITY * BYTES_PER_ELEM);

            // Zero the memory
            uu.setMemory(timePtr, CAPACITY * BYTES_PER_ELEM, (byte) 0);
            uu.setMemory(actPtr, CAPACITY * BYTES_PER_ELEM, (byte) 0);
            uu.setMemory(cycPtr, CAPACITY * BYTES_PER_ELEM, (byte) 0);
            uu.setMemory(callPtr, CAPACITY * BYTES_PER_ELEM, (byte) 0);

            TIME_ADDR.set(timePtr);
            ACT_ADDR.set(actPtr);
            CYC_ADDR.set(cycPtr);
            CALL_ADDR.set(callPtr);
        }
    }

    /**
     * Ensures there is a single shared off-heap 8-byte cell allocated for
     * storing the RDPMC ECX value. If it already exists, returns it;
     * otherwise allocates and zero-initializes it.
     */
    // public static long allocatePmcCellIfMissing() {
    //     long cur = PMC_ADDR.get();
    //     if (cur != 0L) {
    //         return cur;
    //     }
    //     synchronized (BuboNativeBuffers.class) {
    //         cur = PMC_ADDR.get();
    //         if (cur != 0L) {
    //             return cur;
    //         }
    //         Unsafe uu = u();
    //         long pmcPtr = uu.allocateMemory(8);
    //         uu.setMemory(pmcPtr, 8, (byte) 0);
    //         PMC_ADDR.set(pmcPtr);
    //         return pmcPtr;
    //     }
    // }

    // /**
    //  * Writes the given 64-bit ECX value into the shared PMC cell.
    //  * Allocates the cell first if it does not yet exist.
    //  */
    // public static void writePmcCell(long value) {
    //     long ptr = PMC_ADDR.get();
    //     if (ptr == 0L) {
    //         ptr = allocatePmcCellIfMissing();
    //     }
    //     u().putLong(ptr, value);
    // }

    /**
     * Provides a local fallback initialization: if no shared cell exists,
     * allocates one and writes a default RDPMC index (fixed counter #1:
     * core cycles → ECX = (1 << 30) | 1). Used when no native setup has run.
     */
    // private static void ensurePmcInitialized() {
    //     if (PMC_ADDR.get() != 0L) {
    //         return;
    //     }
    //     synchronized (BuboNativeBuffers.class) {
    //         if (PMC_ADDR.get() != 0L) {
    //             return;
    //         }
    //         Unsafe uu = u();
    //         long pmcPtr = uu.allocateMemory(8);

    //         // fixed counter #1 (core cycles): (1 << 30) | 1
    //         long rdpmcIndex = (1L << 30) | 1L;
    //         uu.putLong(pmcPtr, rdpmcIndex);

    //         PMC_ADDR.set(pmcPtr);
    //     }
    // }

    /**
     * Returns the address (pointer) of the shared PMC index cell.
     * Guarantees that the cell exists before returning.
     */
    // public static long pmcIndexPtr() {
    //     //ensurePmcInitialized();
    //     return PMC_ADDR.get();
    // }

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

    public static long readTimeAt(int idx) {
        ensureInitialized();
        return u().getLong(TIME_ADDR.get() + (((long) idx) << 3));
    }

    public static long readActivationAt(int idx) {
        ensureInitialized();
        return u().getLong(ACT_ADDR.get() + (((long) idx) << 3));
    }

    public static long readCyclesAt(int idx) {
        ensureInitialized();
        return u().getLong(CYC_ADDR.get() + (((long) idx) << 3));
    }

    public static long readCallSiteAt(int idx) {
        ensureInitialized();
        return u().getLong(CALL_ADDR.get() + (((long) idx) << 3));
    }

    /** Count non-zero time values up to a bound. */
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
