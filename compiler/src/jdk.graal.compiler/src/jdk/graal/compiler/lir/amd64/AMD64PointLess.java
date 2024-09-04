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
import jdk.vm.ci.code.Register;
import jdk.graal.compiler.asm.amd64.AMD64Address;
import static jdk.vm.ci.amd64.AMD64.rax;;

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

        // Save the current stack pointer (rsp) and align it to 16 bytes.
        asm.subq(AMD64.rsp, 32);  // Adjust stack pointer for local storage (ensure 16-byte alignment)
        
        // Push xmm0 (store xmm0 to the stack)
        asm.vmovdqu(new AMD64Address(AMD64.rsp, 0), AMD64.xmm0); // Store xmm0 to memory at rsp
        
        // Pop xmm0 (restore xmm0 from the stack)
        asm.vmovdqu(AMD64.xmm0, new AMD64Address(AMD64.rsp, 0)); // Load xmm0 from memory at rsp
        
        // Restore the stack pointer after the operation
        asm.addq(AMD64.rsp, 32);  // Restore the stack pointer
    
    }
}
