/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.nfi.backend.panama;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.nfi.backend.panama.PanamaSignature.PanamaSignatureBuilder;
import com.oracle.truffle.nfi.backend.spi.NFIBackend;
import com.oracle.truffle.nfi.backend.spi.NFIBackendLibrary;
import com.oracle.truffle.nfi.backend.spi.types.NativeLibraryDescriptor;
import com.oracle.truffle.nfi.backend.spi.types.NativeSimpleType;
import com.oracle.truffle.nfi.backend.spi.util.ProfiledArrayBuilder.ArrayBuilderFactory;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SymbolLookup;

@ExportLibrary(NFIBackendLibrary.class)
@SuppressWarnings("static-method")
final class PanamaNFIBackend implements NFIBackend {

    private final PanamaNFILanguage language;

    PanamaNFIBackend(PanamaNFILanguage language) {
        this.language = language;
    }

    @Override
    public CallTarget parse(NativeLibraryDescriptor descriptor) {
        RootNode ret;
        if (descriptor.isDefaultLibrary()) {
            ret = new LoadDefaultNode(language);
        } else {
            // TODO: flags
            ret = new LoadLibraryNode(language, descriptor.getFilename());
        }
        return ret.getCallTarget();
    }

    private static class LoadLibraryNode extends RootNode {

        private final String name;

        protected LoadLibraryNode(PanamaNFILanguage language, String name) {
            super(language);
            this.name = name;
        }

        @TruffleBoundary
        private SymbolLookup doLoad() {
            MemorySession session = MemorySession.openImplicit();
            return SymbolLookup.libraryLookup(name, session);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return PanamaLibrary.create(doLoad());
        }
    }

    private static class LoadDefaultNode extends RootNode {

        protected LoadDefaultNode(PanamaNFILanguage language) {
            super(language);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return PanamaLibrary.createDefault();
        }
    }

    @ExportMessage
    Object getSimpleType(NativeSimpleType type) {
        return new PanamaType(type);
    }

    @ExportMessage
    Object getArrayType(NativeSimpleType type) {
        // TODO
        return null;
    }

    @ExportMessage
    Object getEnvType(@CachedLibrary("this") InteropLibrary self) {
        return PanamaNFIContext.get(self).lookupEnvType();
    }

    @ExportMessage
    Object createSignatureBuilder(
                    @CachedLibrary("this") NFIBackendLibrary self,
                    @Cached BranchProfile error,
                    @Cached ArrayBuilderFactory builderFactory) {
        if (!PanamaNFIContext.get(self).env.isNativeAccessAllowed()) {
            error.enter();
            throw new NFIError("Access to native code is not allowed by the host environment.", self);
        }
        return new PanamaSignatureBuilder(builderFactory);
    }
}
