package jdk.graal.compiler.hotspot.meta.Bubo;

import jdk.internal.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * native buffers for Bubo instrumentation.
 */
public final class BuboNativeBuffers {
    private static volatile Unsafe U;

    // Pointers to native memory blocks:
    private static volatile long TIME_PTR;
    private static volatile long ACT_PTR;
    private static volatile long CYC_PTR;
    private static volatile long CALL_PTR;

    private static final int CAPACITY = 200_000;

    private static final long BYTES_PER_ELEM = Long.BYTES;

    public static void ensureInitialized() {
        if (TIME_PTR != 0L) return;
        synchronized (BuboNativeBuffers.class) {
            if (TIME_PTR != 0L) return;
            Unsafe u = u();
            TIME_PTR = U.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            ACT_PTR  = U.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            CYC_PTR  = U.allocateMemory(CAPACITY * BYTES_PER_ELEM);
            CALL_PTR = U.allocateMemory(CAPACITY * BYTES_PER_ELEM);

            // put zeroes in the memory so that it is all allocated up front
            U.setMemory(TIME_PTR, CAPACITY * BYTES_PER_ELEM, (byte)0);
            U.setMemory(ACT_PTR,  CAPACITY * BYTES_PER_ELEM, (byte)0);
            U.setMemory(CYC_PTR,  CAPACITY * BYTES_PER_ELEM, (byte)0);
            U.setMemory(CALL_PTR, CAPACITY * BYTES_PER_ELEM, (byte)0);
        }
    }

    public static long timePtr()       { ensureInitialized(); return TIME_PTR; }
    public static long activationPtr() { ensureInitialized(); return ACT_PTR; }
    public static long cyclesPtr()     { ensureInitialized(); return CYC_PTR; }
    public static long callSitePtr()   { ensureInitialized(); return CALL_PTR; }

    public static void freeAll() {
        synchronized (BuboNativeBuffers.class) {
            if (TIME_PTR != 0L) { U.freeMemory(TIME_PTR); TIME_PTR = 0L; }
            if (ACT_PTR  != 0L) { U.freeMemory(ACT_PTR);  ACT_PTR  = 0L; }
            if (CYC_PTR  != 0L) { U.freeMemory(CYC_PTR);  CYC_PTR  = 0L; }
            if (CALL_PTR != 0L) { U.freeMemory(CALL_PTR); CALL_PTR = 0L; }
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

    public static int capacity() { return CAPACITY; }

    public static long readTimeAt(int idx) {
        ensureInitialized();
        return u().getLong(TIME_PTR + ((long) idx << 3));
    }
    public static long readActivationAt(int idx) {
        ensureInitialized();
        return u().getLong(ACT_PTR + ((long) idx << 3));
    }
    public static long readCyclesAt(int idx) {
        ensureInitialized();
        return u().getLong(CYC_PTR + ((long) idx << 3));
    }
    public static long readCallSiteAt(int idx) {
        ensureInitialized();
        return u().getLong(CALL_PTR + ((long) idx << 3));
    }

    /** count non-zero values up to a bound. */
    public static int countNonZeroTime(int upToExclusive) {
        ensureInitialized();
        int n = Math.min(upToExclusive, CAPACITY);
        int count = 0;
        long base = TIME_PTR;
        Unsafe uu = u();
        for (int i = 0; i < n; i++) {
            if (uu.getLong(base + ((long) i << 3)) != 0L) count++;
        }
        return count;
    }


    private BuboNativeBuffers() {}


}
