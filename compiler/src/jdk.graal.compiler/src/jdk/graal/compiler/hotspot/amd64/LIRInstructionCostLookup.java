package jdk.graal.compiler.hotspot.amd64;

import java.util.HashMap;
import java.util.Map;

public class LIRInstructionCostLookup {
    // Static map to hold the class names and their corresponding costs
    private static final Map<String, Integer> CLASS_COST_MAP = new HashMap<>();

    static {
        // Populate the map with class names and their costs
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBlend$VexBlendOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$TestBranchOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$CmpConstBranchOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$TestConstBranchOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ConvertFloatToIntegerOp", 4);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.StandardOp$LabelOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$BitTestAndBranchOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotSafepointOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$BranchOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotIndirectCallOp", 3);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotStrategySwitchOp", 11);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$CondSetOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$HashTableSwitchOp", 9);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotCRuntimeCallPrologueOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryMROp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$AMD64StackMove", 4);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Binary$MemoryTwoOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64MathPowOp", 1022);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryConstOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$TestByteBranchOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$CondMoveOp", 5);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64SaveRegistersOp", 27);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ShiftOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64SignExtendOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotPatchReturnAddressOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotMove$HotSpotLoadMetaspaceConstantOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBinary$AVXBinaryMemoryOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64MathCosOp", 412);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Binary$DataTwoOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64DeoptimizeOp", 3);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBinary$AVXBinaryOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBinary$AVXBinaryConstFloatOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$Op", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64MathLogOp", 138);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ZeroMemoryOp", 6);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$ConstOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotspotDirectVirtualCallOp", 4);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64MathSinOp", 415);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64PrefetchOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotUnwindOp", 4);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$RangeTableSwitchOp", 9);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.g1.AMD64G1PreWriteBarrierOp", 12);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Binary$CommutativeTwoOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotDeoptimizeCallerOp", 3);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryRMOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64MulDivOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64VectorizedMismatchOp", 113);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Unary$MemoryOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$CmpBranchOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$CompressPointerOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ByteSwapOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Call$DirectNearForeignCallOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$NullCheckOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Unary$MROp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotDirectStaticCallOp", 3);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Unary$MOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryVMConstOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Unary$RMOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp", 6);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$MoveFromConstOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Binary$ConstOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64VectorizedHashCodeOp", 143);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64StringUTF16CompressOp", 68);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotMove$HotSpotLoadObjectConstantOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$CompareAndSwapOp", 5);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$StackLeaOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$AMD64MultiStackMove", 6);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$MembarOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$FloatBranchOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$LeaOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$FloatCondMoveOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Binary$RMIOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64RestoreRegistersOp", 27);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$VMConstOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ArrayIndexOfOp", 128);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$AtomicReadAndAddOp", 4);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ControlFlow$FloatCondSetOp", 2);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64UnaryConsumer$MemoryOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$MoveToRegOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.StandardOp$JumpOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$MoveFromRegOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Move$UncompressPointerOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.vector.AMD64VectorFloatCompareOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64Binary$TwoOp", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64NormalizedUnsignedCompareOp", 5);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64ArrayEqualsOp", 17);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotCRuntimeCallEpilogueOp", 3);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.g1.AMD64G1PostWriteBarrierOp", 22);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64VZeroUpper", 1);
        CLASS_COST_MAP.put("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotJumpToExceptionHandlerInCallerOp", 4);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64SHA1Op", 245);
        CLASS_COST_MAP.put("jdk.graal.compiler.lir.amd64.AMD64StringLatin1InflateOp", 46);
    }

    /**
     * Method to look up the cost of a given class.
     *
     * @param className the fully qualified name of the class
     * @return the cost associated with the class, or -1 if the class is not found
     */
    public static int getCost(String className) {
        String sanitizedClassName = className.replaceFirst("class ", "");
        //if (!CLASS_COST_MAP.containsKey(sanitizedClassName.trim())) {
        //    System.out.println("Have no info for " + sanitizedClassName);
        // }
        return CLASS_COST_MAP.getOrDefault(sanitizedClassName.trim(), 1);
    }
}
