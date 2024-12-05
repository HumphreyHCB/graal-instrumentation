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
 * Emits a pointless vector Shuffle.
 */
@Opcode("Pointlesss")
public final class AMD64PointLesss extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64PointLesss> TYPE = LIRInstructionClass.create(AMD64PointLesss.class);

    int amount;

    public AMD64PointLesss(int amount) {
        super(TYPE);
        this.amount = amount;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm) {

        if (amount >= 500) {
            for (int index = 0; index < amount - 499; index++) {
                asm.movq(AMD64.cpuRegisters[0],
                        AMD64.cpuRegisters[0]);
            }
        } else {
            for (int index = 0; index < amount; index++) {
                asm.movq(AMD64.cpuRegisters[index % AMD64.cpuRegisters.length],
                        AMD64.cpuRegisters[index % AMD64.cpuRegisters.length]);
            }
        }

    }
}
