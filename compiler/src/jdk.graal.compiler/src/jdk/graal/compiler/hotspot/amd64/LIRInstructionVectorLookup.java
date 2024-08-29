package jdk.graal.compiler.hotspot.amd64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.util.json.JsonParser;

import java.util.HashSet;
import java.util.Set;

public class LIRInstructionVectorLookup {

    private static final Set<String> lirInstructionClasses = new HashSet<>();

    static {
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBlend$VexBlendOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64ConvertFloatToIntegerOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryMROp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Move$AMD64StackMove");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64MathPowOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64SaveRegistersOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBinary$AVXBinaryMemoryOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Binary$DataTwoOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBinary$AVXBinaryOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.vector.AMD64VectorBinary$AVXBinaryConstFloatOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$Op");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64MathLogOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryRMOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64VectorizedMismatchOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Unary$MemoryOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64StringLatin1InflateOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Unary$MROp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Unary$RMOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Move$MoveFromConstOp");
        lirInstructionClasses.add("jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64VectorizedHashCodeOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64StringUTF16CompressOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64RestoreRegistersOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64ArrayIndexOfOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Move$MoveToRegOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64Move$MoveFromRegOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.vector.AMD64VectorFloatCompareOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64ArrayEqualsOp");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64VZeroUpper");
        lirInstructionClasses.add("jdk.graal.compiler.lir.amd64.AMD64SHA1Op");
    }

    public static boolean containsClassName(String className) {
        String sanitizedClassName = className.replaceFirst("class ", "").trim();
        return lirInstructionClasses.contains(sanitizedClassName);
    }

}
