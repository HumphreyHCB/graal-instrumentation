/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.nodes.func;

import java.util.LinkedList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.memory.LLVMNativeFunctions;

public final class LLVMBeginCatchNode extends LLVMExpressionNode {

    @Child private LLVMExpressionNode exceptionPointer;
    @Child private LLVMNativeFunctions.SulongGetThrownObjectNode getThrownObject;
    @Child private LLVMNativeFunctions.SulongIncrementHandlerCountNode handlerCount;
    private final LinkedList<LLVMAddress> caughtExceptionStack;

    public LLVMBeginCatchNode(LLVMContext context, LLVMExpressionNode exceptionPointer) {
        this.exceptionPointer = exceptionPointer;
        this.getThrownObject = context.getNativeFunctions().createGetThrownObject();
        this.handlerCount = context.getNativeFunctions().createIncrementHandlerCount();
        this.caughtExceptionStack = context.getCaughtExceptionStack();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            LLVMAddress ptr = exceptionPointer.executeLLVMAddress(frame);
            LLVMAddress thrownObj = getThrownObject.getThrownObject(ptr);
            handlerCount.inc(ptr);
            pushExceptionToStack(ptr);
            return thrownObj;
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IllegalStateException(e);
        }
    }

    @TruffleBoundary
    private void pushExceptionToStack(LLVMAddress exc) {
        if (caughtExceptionStack.size() > 0 && caughtExceptionStack.peek().getVal() == exc.getVal()) {
            // exception already on stack
            return;
        }
        caughtExceptionStack.push(exc);
    }

}
