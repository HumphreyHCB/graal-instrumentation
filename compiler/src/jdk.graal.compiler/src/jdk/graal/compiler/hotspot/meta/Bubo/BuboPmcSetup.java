package jdk.graal.compiler.hotspot.meta.Bubo;

import jdk.graal.compiler.serviceprovider.GlobalAtomicLong;
import jdk.internal.misc.Unsafe;

public final class BuboPmcSetup {

    // shared across isolates: holds the *address* of the 8-byte cell
    private static final GlobalAtomicLong PMC_CELL_PTR =
            new GlobalAtomicLong("Bubo.PMC_IDX_PTR", 0L);

    // guard so we only do native perf setup once
    private static final GlobalAtomicLong PMC_INIT =
            new GlobalAtomicLong("Bubo.PMC_INIT", 0L);

    private static final String LIB_PATH = "/home/hb478/repos/graal-instrumentation/libbubopmc.so";

    private static native int nativeSetupPmc();

    public static void ensureInitialized() {
        if (PMC_INIT.get() != 0L) {
            return;
        }
        synchronized (BuboPmcSetup.class) {
            if (PMC_INIT.get() != 0L) {
                return;
            }

            // 1) make sure we have a cell to write ECX into
            long cell = PMC_CELL_PTR.get();
            if (cell == 0L) {
                Unsafe u = Unsafe.getUnsafe();
                long ptr = u.allocateMemory(8);
                u.setMemory(ptr, 8, (byte) 0);
                PMC_CELL_PTR.set(ptr);
                cell = ptr;
            }

            // 2) load native lib
            try {
                System.load(LIB_PATH);
            } catch (Throwable t) {
                // fallback: leave cell = 0 → rdpmc path can detect this
                System.out.println("BuboPmcSetup: failed to load " + LIB_PATH + " : " + t);
                PMC_INIT.set(1L);
                return;
            }

            // 3) ask native to create perf event + tell us index
            int idx;
            try {
                idx = nativeSetupPmc();
            } catch (Throwable t) {
                System.out.println("BuboPmcSetup: nativeSetupPmc() threw: " + t);
                PMC_INIT.set(1L);
                return;
            }

            // if native said "no rdpmc", just leave the cell at 0
            if (idx == 0) {
                PMC_INIT.set(1L);
                System.out.println("BuboPmcSetup: kernel did not allow user RDPMC; ECX=0");
                return;
            }

            long ecx = (1L << 30) | (idx & 0xffffffffL);

            // 4) write ECX into the cell
            Unsafe u = Unsafe.getUnsafe();
            u.putLong(cell, ecx);

            PMC_INIT.set(1L);
            System.out.println("BuboPmcSetup: RDPMC ECX=0x" + Long.toHexString(ecx)
                    + " stored at cell=0x" + Long.toHexString(cell));
        }
    }

    /**
     * Returns the pointer to the 8-byte cell that contains the ECX value
     * to use for RDPMC. Returns 0 if setup didn’t succeed.
     */
    public static long pmcIndexPtr() {
        // make sure init ran
       // ensureInitialized();
        return PMC_CELL_PTR.get();
    }

    private BuboPmcSetup() {}
}
