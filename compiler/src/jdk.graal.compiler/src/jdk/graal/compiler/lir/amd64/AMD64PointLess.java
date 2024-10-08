/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.graal.compiler.lir.amd64;

import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.Opcode;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.vm.ci.amd64.AMD64;
import jdk.graal.compiler.asm.amd64.AMD64Address;;

/**
 * Emits a push and pop.
 */
@Opcode("PushAndPop")
public final class AMD64PointLess extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64PointLess> TYPE = LIRInstructionClass.create(AMD64PointLess.class);

    public AMD64PointLess() {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm) {    

        // // Save the current stack pointer (rsp) and align it to 32 bytes.
        // asm.subq(AMD64.rsp, 32);  // Adjust stack pointer for local storage (ensure 32-byte alignment)
        
        // // Push xmm0 (store xmm0 to the stack)
        // asm.vmovdqu(new AMD64Address(AMD64.rsp, 0), AMD64.xmm0); // Store xmm0 to memory at rsp
        
        // // Pop xmm0 (restore xmm0 from the stack)
        // asm.vmovdqu(AMD64.xmm0, new AMD64Address(AMD64.rsp, 0)); // Load xmm0 from memory at rsp
        
        // // Restore the stack pointer after the operation
        // asm.addq(AMD64.rsp, 32);  // Restore the stack pointer

        // Perform a pointless permutation on ymm0 to waste CPU cycles.
        asm.vmovdqu(AMD64.xmm0, AMD64.xmm0); // Move xmm0 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm1, AMD64.xmm1); // Move xmm1 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm2, AMD64.xmm2); // Move xmm2 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm3, AMD64.xmm3); // Move xmm3 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm4, AMD64.xmm4); // Move xmm4 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm5, AMD64.xmm5); // Move xmm5 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm6, AMD64.xmm6); // Move xmm6 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm7, AMD64.xmm7); // Move xmm7 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm8, AMD64.xmm8); // Move xmm8 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm9, AMD64.xmm9); // Move xmm9 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm10, AMD64.xmm10); // Move xmm10 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm11, AMD64.xmm11); // Move xmm11 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm12, AMD64.xmm12); // Move xmm12 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm13, AMD64.xmm13); // Move xmm13 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm14, AMD64.xmm14); // Move xmm14 into itself, effectively wasting cycles
        asm.vmovdqu(AMD64.xmm15, AMD64.xmm15); // Move xmm15 into itself, effectively wasting cycles

    
    }
}
