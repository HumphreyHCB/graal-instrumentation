/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.graalvm.wasm.parser.validation;

import org.graalvm.wasm.exception.Failure;
import org.graalvm.wasm.exception.WasmException;

public class IfFrame extends ControlFrame {
    private int falseJumpLocation;
    private boolean elseBranch;

    IfFrame(byte[] paramTypes, byte[] resultTypes, int initialStackSize, boolean unreachable, int falseJumpLocation) {
        super(paramTypes, resultTypes, initialStackSize, unreachable);
        this.falseJumpLocation = falseJumpLocation;
        this.elseBranch = false;
    }

    @Override
    public byte[] getLabelTypes() {
        return getResultTypes();
    }

    @Override
    public void enterElse(ParserState state, ExtraDataList extraData, int offset) {
        int endJumpLocation = extraData.addElseLocation();
        extraData.setIfTarget(falseJumpLocation, offset, extraData.getLocation());
        falseJumpLocation = endJumpLocation;
        elseBranch = true;
        state.checkStackAfterFrameExit(this, getResultTypes());
    }

    @Override
    public void exit(ExtraDataList extraData, int offset) {
        if (!elseBranch && getLabelTypeLength() > 0) {
            throw WasmException.format(Failure.TYPE_MISMATCH, "Expected else branch. If with result value requires then and else branch.");
        }
        if (elseBranch) {
            extraData.setElseTarget(falseJumpLocation, offset, extraData.getLocation());
        } else {
            extraData.setIfTarget(falseJumpLocation, offset, extraData.getLocation());
        }
        for (int location : conditionalBranches()) {
            extraData.setConditionalBranchTarget(location, offset, extraData.getLocation(), getInitialStackSize(), getLabelTypeLength());
        }
        for (int location : unconditionalBranches()) {
            extraData.setUnconditionalBranchTarget(location, offset, extraData.getLocation(), getInitialStackSize(), getLabelTypeLength());
        }
    }

}
