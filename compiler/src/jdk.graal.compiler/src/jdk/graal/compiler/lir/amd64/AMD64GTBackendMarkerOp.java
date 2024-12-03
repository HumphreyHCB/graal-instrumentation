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
import jdk.graal.compiler.asm.amd64.AVXKind.AVXSize;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.Opcode;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.vm.ci.amd64.AMD64;
import jdk.graal.compiler.asm.amd64.AMD64Address;;

/**
 * Emits a Pointless vshufps with a shift value of the marker ID,
 * this is used by GT to work out in the VTune report which block is which
 */
@Opcode("GTMarker")
public final class AMD64GTBackendMarkerOp extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64GTBackendMarkerOp> TYPE = LIRInstructionClass
            .create(AMD64GTBackendMarkerOp.class);

    private int blockID; // this ID of the graal block
    private int uniqueID; // this ID will usally be a block ID
    private String compID;

    public AMD64GTBackendMarkerOp(int blockID, int uniqueID, String compID) {
        super(TYPE);
        this.blockID = blockID;
        this.uniqueID = uniqueID;
        this.compID = compID;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm) {
        asm.sfence();

        // Extract lower 8 bits of the marker ID
        int lower8 = blockID & 0xFF;

        // Extract upper 8 bits of the marker ID
        int upper8 = (blockID >> 8) & 0xFF;

        asm.vpblendd(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, lower8, AVXSize.XMM); // Lower 8 bits of the marker ID
        asm.vpblendd(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, upper8, AVXSize.XMM); // Upper 8 bits of the marker ID
        asm.vpblendd(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, uniqueID, AVXSize.XMM); // Upper 8 bits of the marker ID

        // asm.vshufps(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, lower8); // Redundant shuffle operation, results in no change
        // asm.vshufps(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, upper8); // Redundant shuffle operation, results in no change
        // asm.vshufps(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, uniqueID); // Redundant shuffle operation, results in no change
        asm.sfence();

    }
}
