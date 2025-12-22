package jdk.graal.compiler.lir.amd64.Bubo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jdk.graal.compiler.lir.VirtualStackSlot;

public final class BuboProbeFrameInfo {

    // Per-loop “start timestamp” slot
    public final Map<Integer, VirtualStackSlot> loopStartSlots;

    // Shared save slots for START probe (RDTSCToSlot)
    public final VirtualStackSlot startSaveRax;
    public final VirtualStackSlot startSaveRdx;

    // Shared save slots for END probe (WriteDelta)
    public final VirtualStackSlot endSaveR9;
    public final VirtualStackSlot endSaveR10;
    public final VirtualStackSlot endSaveR11;
    public final VirtualStackSlot endSaveRax;
    public final VirtualStackSlot endSaveRdx;

    public BuboProbeFrameInfo(
            Map<Integer, VirtualStackSlot> loopStartSlots,
            VirtualStackSlot startSaveRax,
            VirtualStackSlot startSaveRdx,
            VirtualStackSlot endSaveR9,
            VirtualStackSlot endSaveR10,
            VirtualStackSlot endSaveR11,
            VirtualStackSlot endSaveRax,
            VirtualStackSlot endSaveRdx) {
        this.loopStartSlots = loopStartSlots;
        this.startSaveRax = startSaveRax;
        this.startSaveRdx = startSaveRdx;
        this.endSaveR9 = endSaveR9;
        this.endSaveR10 = endSaveR10;
        this.endSaveR11 = endSaveR11;
        this.endSaveRax = endSaveRax;
        this.endSaveRdx = endSaveRdx;
    }

    // ---- Cache keyed by compilationId ----
    private static final ConcurrentHashMap<Integer, BuboProbeFrameInfo> CACHE = new ConcurrentHashMap<>();

    public static void put(int compilationId, BuboProbeFrameInfo info) {
        CACHE.put(compilationId, info);
    }

    public static BuboProbeFrameInfo get(int compilationId) {
        return CACHE.get(compilationId);
    }

    public static void remove(int compilationId) {
        CACHE.remove(compilationId);
    }
}
