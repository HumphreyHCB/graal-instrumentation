package jdk.graal.compiler.lir.phases;


import java.util.ArrayList;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.debug.Markers.CompilerMarkers;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.HotSpotGraalCompiler;
import jdk.graal.compiler.hotspot.HotSpotGraalRuntime;
import jdk.graal.compiler.lir.ConstantValue;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.StandardOp.BlockEndOp;
import jdk.graal.compiler.lir.StandardOp.LabelOp;
import jdk.graal.compiler.lir.gen.DiagnosticLIRGeneratorTool;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class LIRCompilerDebugAssignmentPhase extends PreAllocationOptimizationPhase {


    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PreAllocationOptimizationContext context) {
        HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) HotSpotJVMCIRuntime.runtime().getCompiler();
       // compiler.getGraalRuntime().getHostProviders().getMetaAccess().lookupJavaType(CompilerMarkers.class);
        //LIRGeneratorTool tool = (LIRGeneratorTool) context.diagnosticLirGenTool;
        //MetaAccessProvider meta = tool.getMetaAccess(); 
        //ResolvedJavaType markerType = meta.lookupJavaType(CompilerMarkers.class);
        ResolvedJavaType markerType = compiler.getGraalRuntime().getHostProviders().getMetaAccess().lookupJavaType(CompilerMarkers.class);
        ResolvedJavaMethod stubMethod = markerType.getDeclaredMethods()[0];


        for (int blockId : lirGenRes.getLIR().getBlocks()) {
            if (LIR.isBlockDeleted(blockId)) {
                continue;
            }
            BasicBlock<?> b = lirGenRes.getLIR().getBlockById(blockId);
            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);

            for (LIRInstruction instruction : instructions) {

                if (instruction.getPosition() == null) {
                    NodeSourcePosition position = new NodeSourcePosition(null, null, stubMethod, -1);
                    instruction.setPosition(position);
                   
                }

            }

        }
        
    }


}

