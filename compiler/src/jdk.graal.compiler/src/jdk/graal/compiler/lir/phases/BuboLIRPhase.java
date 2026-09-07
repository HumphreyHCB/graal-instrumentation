package jdk.graal.compiler.lir.phases;

import static jdk.graal.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.AbstractControlFlowGraph;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.core.common.cfg.CFGLoop;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeLoopLoopCallCountCache;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeLoopSourceCache;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64Call.CallOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopEndOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboIncActivationOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot_SpillFixed;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC_SpillFixed;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.options.NestedBooleanOptionKey;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionType;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.TargetDescription;


import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;


public final class BuboLIRPhase extends PreAllocationOptimizationPhase {

    public static class Options {
        @Option(help = "Enable Bubo Lir Phase.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey BuboLIRPhase = new NestedBooleanOptionKey(LIROptimization, false);
    }

    private static final class MarkerPos {
        final int blockIndex;
        final int insnIndex;
        final int loopId;
        final NodeSourcePosition pos;
        final boolean LoopStart;

        MarkerPos(int blockIndex, int insnIndex, int loopId, NodeSourcePosition pos, boolean LoopStart) {
            this.blockIndex = blockIndex;
            this.insnIndex = insnIndex;
            this.loopId = loopId;
            this.pos = pos;
            this.LoopStart = LoopStart;
        }

        @Override
        public String toString() {
            return "b=" + blockIndex + ", insn=" + insnIndex;
        }
    }

    //private static ResolvedJavaMethod stringHashCode;      // cached String.hashCode()I


    @Override
    protected void run(TargetDescription target,
            LIRGenerationResult lirGenRes,
            PreAllocationOptimizationContext context) {
        if (shouldSkip(lirGenRes)) {
            return;
        }
        // // Tee up String.hashCode()I so this.stringHashCode is ready for later use.
        // if (this.stringHashCode == null) {
        //     HotSpotResolvedObjectType accessingType = null;

        //     // Fast path: try to find any NodeSourcePosition from existing LIR ops.
        //     LIR tmpLir = lirGenRes.getLIR();
        //     BasicBlock<?>[] tmpBlocks = tmpLir.getControlFlowGraph().getBlocks();

        //     outer:
        //     for (int b = 0; b < tmpBlocks.length; b++) {
        //         List<LIRInstruction> insns = tmpLir.getLIRforBlock(tmpBlocks[b]);
        //         for (int i = 0; i < insns.size(); i++) {
        //             NodeSourcePosition p = insns.get(i).getPosition();
        //             if (p != null) {
        //                 accessingType = findHotSpotAccessingType(p);
        //                 if (accessingType != null) {
        //                     break outer;
        //                 }
        //             }
        //         }
        //     }

        //     // Resolve and cache on the instance (and your existing static cache gets filled too).
        //     if (accessingType != null) {
        //         this.stringHashCode = resolveStringHashCode(accessingType);
        //     } else {
        //             throw new IllegalStateException("Could not find a HotSpot accessing type to resolve String.hashCode()I");
                
        //     }
        // }


        final LIR lir = lirGenRes.getLIR();
        final LIRGeneratorTool lirGen = context.lirGen;
        final long baseAddress = BuboNativeBuffers.activationPtr();
        final int compilationId = lirGenRes.getCompilationId();

        List<MarkerPos> markers = new ArrayList<>();
        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

        //System.out.println("--- BuboLIRPhase for compilationId " + lirGenRes.getCompilationUnitName() + " ---");

        // collect all markers, all starts and ends
        for (int block = 0; block < blocks.length; block++) {
            List<LIRInstruction> insns = lir.getLIRforBlock(blocks[block]);
            for (int instruction = 0; instruction < insns.size(); instruction++) {
                LIRInstruction op = insns.get(instruction);

                if (op instanceof AMD64LoopStartOp) {
                    
                    AMD64LoopStartOp StartOp = (AMD64LoopStartOp) op;
                    // if (StartOp.loopId == 0) {
                    //     System.out.println("Found 0 op start");
                    // }
                    markers.add(new MarkerPos(block, instruction, StartOp.loopId, StartOp.position, true));
                } else if (op instanceof AMD64LoopEndOp) {
                    
                    AMD64LoopEndOp EndOp = (AMD64LoopEndOp) op;
                    // if (EndOp.loopId == 0) {
                    //     System.out.println("Found 0 op end");
                    // }
                    markers.add(new MarkerPos(block, instruction, EndOp.loopId, EndOp.position, false));
                }
            }
        }

        

        if (markers.isEmpty()) {
            return;
        }

        // allocate spill
        Map<Integer, VirtualStackSlot> loopSlots = new HashMap<>();
        for (MarkerPos marker : markers) {
            if (marker.LoopStart && !loopSlots.containsKey(marker.loopId)) {
                VirtualStackSlot slot = lirGenRes.getFrameMapBuilder()
                        .allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
                loopSlots.put(marker.loopId, slot);
            }
        }

        FindAllCallsInloop(lir, lirGen, lirGenRes, compilationId);

        AbstractControlFlowGraph<?> cfg = lir.getControlFlowGraph();

        Map<Integer, CFGLoop<?>> loopIdMap = mapLoopIdsToLoops(cfg, lir);

        ensureEndMarkersFromCFGIfMissing(
                lir,
                cfg,
                loopIdMap,
                markers, 0);
        /// here

        // insert starts only
        for (MarkerPos marker : markers) {
            if (marker.LoopStart) {
                // if (isNestedLoop(marker.loopId, mapLoopIdsToLoops(lir.getControlFlowGraph(), lir))) {
                //     continue;  
                // }
                insertStartBeforeMarker(lir, lirGen, loopSlots.get(marker.loopId), compilationId, marker, lirGenRes);
            }
        }

        // insert ends only for instrumentable ids
        for (MarkerPos marker : markers) {
            if (!marker.LoopStart) {
                //  if (isNestedLoop(marker.loopId, mapLoopIdsToLoops(lir.getControlFlowGraph(), lir))) {
                //     continue;   
                // }
                instrumentLoopEnds(lir, lirGen, loopSlots.get(marker.loopId), baseAddress, compilationId, marker, lirGenRes);
            }
        }

        

    }

    private static boolean shouldSkip(LIRGenerationResult lirGenRes) {
        String name = lirGenRes.getCompilationUnitName();
        return name.contains("Stub") || name.contains("HotSpotOSRCompilation");
    }

    static Map<Integer, String> loopStartSources = new HashMap<>();

    private static void insertStartBeforeMarker(LIR lir,
            LIRGeneratorTool lirGen,
            VirtualStackSlot slot, int compilationId,
            MarkerPos marker, LIRGenerationResult lirGenRes) {

        BasicBlock<?> block = lir.getControlFlowGraph().getBlocks()[marker.blockIndex];
        List<LIRInstruction> insns = lir.getLIRforBlock(block);

        if (marker.pos == null) {
            loopStartSources.put(marker.loopId, "Source Missing");
        } else {
            loopStartSources.put(marker.loopId, marker.pos.toString("-"));
        }

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);
       AMD64BuboRDTSCToSlot toSlot =  new AMD64BuboRDTSCToSlot(lirGen, slot,marker.loopId);
       //toSlot.setPosition(new NodeSourcePosition(null,null, stringHashCode, -1));
        buf.append(insns.size()-1, toSlot);
       // buf.append(marker.insnIndex, new AMD64BuboRDTSCToSlot(lirGen, slot,marker.loopId));
        //buf.append(insns.size()-1, new AMD64BuboIncActivationOp(lirGen, compilationId, marker.loopId));
        buf.finish();
    }

    private static void instrumentLoopEnds(LIR lir,
            LIRGeneratorTool lirGen,
            VirtualStackSlot slot,
            long baseAddress,
            int compilationId,
            MarkerPos marker, LIRGenerationResult lirGenerationResult) {

        BasicBlock<?> block = lir.getControlFlowGraph().getBlocks()[marker.blockIndex];
        List<LIRInstruction> insns = lir.getLIRforBlock(block);

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);

        AMD64BuboWriteDeltaRDTSC endDelta = new AMD64BuboWriteDeltaRDTSC(
                lirGen,
                slot,
                baseAddress,
                compilationId,
                marker.loopId,
                true);
        
        //endDelta.setPosition(new NodeSourcePosition(null,null, stringHashCode, -1));
        buf.append(1, endDelta);

        String startSrc = loopStartSources.get(marker.loopId);
        String endSrc;
        if (marker.pos == null) {
            endSrc = " No Source";
        } else {
            endSrc = marker.pos.toString("-");
        }

        String combined = startSrc + " | " + endSrc ;

        BuboNativeLoopSourceCache.add(compilationId, marker.loopId, combined);

        buf.finish();

    }

    static Map<Integer, Integer> loopCallCount = new HashMap<>();

    private void FindAllCallsInloop(LIR lir,
            LIRGeneratorTool lirGen, LIRGenerationResult lirGenRes,  int compilationId) {

        AbstractControlFlowGraph<?> cfg = lir.getControlFlowGraph();

        Map<Integer, CFGLoop<?>> loopIdMap = orderLoopsByNesting(mapLoopIdsToLoops(cfg, lir));
        
        for (Map.Entry<Integer, CFGLoop<?>> entry : loopIdMap.entrySet()) {
            Integer LoopID = entry.getKey();
            CFGLoop<?> loop = entry.getValue();
            int callCount = 0;
            List<BasicBlock<?>> loopBlocks = (List<BasicBlock<?>>) loop.getBlocks();

            for (BasicBlock<?> block : loopBlocks) {
                @SuppressWarnings("unchecked")
                ArrayList<LIRInstruction> instructions =  (ArrayList<LIRInstruction>) lir.getLIRforBlock(block);

                for (LIRInstruction instr : instructions) {
                    if (instr instanceof CallOp) {
                        callCount++;
                        // CallOp op = (CallOp) instr;
                        // System.out.println(lirGenRes.getCompilationUnitName() + " : Call found in loop " + LoopID + " : " + instr.getClass());
                    }
                }
            }
            BuboNativeLoopLoopCallCountCache.add(compilationId, LoopID, callCount);
        }


    }


        private Map<Integer, CFGLoop<?>> orderLoopsByNesting(Map<Integer, CFGLoop<?>> loopIdMap) {
        if (loopIdMap == null || loopIdMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // Copy entries into a list for sorting
        List<Map.Entry<Integer, CFGLoop<?>>> entries = new ArrayList<>(loopIdMap.entrySet());

        // Sort by loop depth (outer → inner).
        // Tie-breaker: loopId ascending to keep ordering stable.
        entries.sort((e1, e2) -> {
            int d1 = e1.getValue().getDepth();
            int d2 = e2.getValue().getDepth();

            if (d1 != d2) {
                return Integer.compare(d1, d2);
            }
            return Integer.compare(e1.getKey(), e2.getKey());
        });

        // Build ordered map preserving sorted order
        Map<Integer, CFGLoop<?>> ordered = new LinkedHashMap<>();
        for (Map.Entry<Integer, CFGLoop<?>> e : entries) {
            ordered.put(e.getKey(), e.getValue());
        }

        return ordered;
    }

    private Map<Integer, CFGLoop<?>> mapLoopIdsToLoops(AbstractControlFlowGraph<?> cfg, LIR lir) {
        BasicBlock<?>[] blocks = cfg.getBlocks();

        // loopId (from AMD64LoopStartOp) -> CFGLoop (from first successor)
        Map<Integer, CFGLoop<?>> loopsFromStarts = new HashMap<>();

        for (BasicBlock<?> block : blocks) {
            @SuppressWarnings("unchecked")
            List<LIRInstruction> lirList = (List<LIRInstruction>) lir.getLIRforBlock(block);
            if (lirList == null || lirList.isEmpty()) {
                continue;
            }

            AMD64LoopStartOp loopStart = null;
            for (LIRInstruction instr : lirList) {
                if (instr instanceof AMD64LoopStartOp) {
                    loopStart = (AMD64LoopStartOp) instr;
                    break;
                }
            }

            if (loopStart == null) {
                continue;
            }

            CFGLoop<?> succLoop = block.getSuccessorAt(0).getLoop();
            if (succLoop == null) {
                continue;
            }

            loopsFromStarts.put(loopStart.loopId, succLoop);
        }

        return loopsFromStarts;
    }

private static boolean isNestedLoop(int loopId, Map<Integer, CFGLoop<?>> loopsByNesting) {
    if (loopsByNesting == null) {
        return false;
    }

    CFGLoop<?> loop = loopsByNesting.get(loopId);
    if (loop == null) {
        return false;
    }

    CFGLoop<?> parent = loop.getParent(); // or getParentLoop()

    // No parent → not nested
    if (parent == null) {
        return false;
    }

    // // If parent is the outermost loop (usually loopId 0), treat as not nested
    // for (Map.Entry<Integer, CFGLoop<?>> e : loopsByNesting.entrySet()) {
    //     if (e.getKey() == 0 && e.getValue() == parent) {
    //         return false;
    //     }
    // }

    // Otherwise, this loop is genuinely nested
    return true;
}

private static void ensureEndMarkersFromCFGIfMissing(
        LIR lir,
        AbstractControlFlowGraph<?> cfg,
        Map<Integer, CFGLoop<?>> loopIdMap,
        List<MarkerPos> markers,
        int loopId) {

    // Do we already have at least one end marker for this loopId?
    boolean hasEnd = false;
    for (MarkerPos m : markers) {
        if (!m.LoopStart && m.loopId == loopId) {
            hasEnd = true;
            break;
        }
    }
    if (hasEnd) {
        return;
    }

    CFGLoop<?> loop = loopIdMap.get(loopId);
    if (loop == null) {
        return;
    }

    // Prefer CFGLoop.getLoopExits(), fall back to getNaturalExits() if empty.
    List<? extends BasicBlock<?>> exits = loop.getLoopExits();
    if (exits == null || exits.isEmpty()) {
        exits = loop.getNaturalExits();
    }
    if (exits == null || exits.isEmpty()) {
        return;
    }

    // Map BasicBlock -> blockIndex in cfg.getBlocks()
    BasicBlock<?>[] blocks = cfg.getBlocks();
    Map<BasicBlock<?>, Integer> blockToIndex = new HashMap<>(blocks.length * 2);
    for (int i = 0; i < blocks.length; i++) {
        blockToIndex.put(blocks[i], i);
    }

    for (BasicBlock<?> exitBlock : exits) {
        Integer blockIndex = blockToIndex.get(exitBlock);
        if (blockIndex == null) {
            continue;
        }

        List<LIRInstruction> insns = lir.getLIRforBlock(exitBlock);
        if (insns == null || insns.isEmpty()) {
            continue;
        }

        // Pick an insertion point inside the exit block.
        // We want the end probe to run when we are exiting the loop.
        // A decent generic choice is just before the block terminator (often the last LIR op).
        int insnIndex = Math.max(0, insns.size() - 1);

        // Try to steal a position from the chosen instruction, if any.
        NodeSourcePosition pos = insns.get(insnIndex).getPosition();

        markers.add(new MarkerPos(blockIndex, insnIndex, loopId, pos, false));
    }
}




    // Minimal helper: create a NodeSourcePosition that references java/lang/String.hashCode()I
    // (which implies the declaring class is Ljava/lang/String;).
    //
    // Safe for native-image: no <clinit> work, the lookup happens lazily at runtime.


//     private static ResolvedJavaMethod resolveStringHashCode(HotSpotResolvedObjectType accessingType) {
//         HotSpotJVMCIRuntime rt = HotSpotJVMCIRuntime.runtime();

//         ResolvedJavaType stringType =
//                 (ResolvedJavaType) rt.lookupType("Ljava/lang/String;", accessingType, true);

//         for (ResolvedJavaMethod m : stringType.getDeclaredMethods()) {
//             if ("hashCode".equals(m.getName())
//                     && m.getSignature().getParameterCount(false) == 0
//                     && "I".equals(m.getSignature().getReturnType(null).getName())) {
//                 return m;
//             }
//         }

//         throw new IllegalStateException("Did not find java/lang/String.hashCode()I");
//     }

//     private static HotSpotResolvedObjectType findHotSpotAccessingType(NodeSourcePosition pos) {
//     for (NodeSourcePosition p = pos; p != null; p = p.getCaller()) {
//         ResolvedJavaMethod m = p.getMethod();
//         if (m == null) {
//             continue;
//         }
//         ResolvedJavaType t = m.getDeclaringClass();
//         if (t instanceof HotSpotResolvedObjectType) {
//             return (HotSpotResolvedObjectType) t;
//         }
//     }
//     return null;
// }



}
