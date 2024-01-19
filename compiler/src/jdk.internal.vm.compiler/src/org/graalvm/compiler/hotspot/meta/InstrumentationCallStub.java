/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.hotspot.meta;

import static org.graalvm.compiler.hotspot.stubs.StubUtil.printf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.graalvm.compiler.api.replacements.Snippet;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import org.graalvm.compiler.hotspot.meta.HotSpotProviders;
import org.graalvm.compiler.hotspot.stubs.SnippetStub;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.java.MethodCallTargetNode;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode;
import org.graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation;
import org.graalvm.compiler.replacements.nodes.UnaryMathIntrinsicNode;
import org.graalvm.compiler.replacements.nodes.UnaryMathIntrinsicNode.UnaryOperation;

import static org.graalvm.compiler.hotspot.meta.BuboCache.pointer;
import static org.graalvm.compiler.hotspot.meta.HotSpotForeignCallDescriptor.Transition.SAFEPOINT;
import static org.graalvm.compiler.hotspot.meta.BuboCache.incPointer;
//import static org.graalvm.compiler.hotspot.meta.BuboCache.Buffer;



import static org.graalvm.compiler.hotspot.replacements.InstrumentationSnippets.instrumentation;

/**
 * 
 */
public class InstrumentationCallStub extends SnippetStub {


    public InstrumentationCallStub(String methodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
        super(methodName, options, providers, linkage);
        //BUBO_ADD


        
    }

    
    @Snippet
    public static void addToBuboCache(long time) {
        //providers.getForeignCalls().invokeJavaMethodStub(options, providers, BUBO_ADD, providers.getConfig().invokeJavaMethodAddress, method);
        printf("\n The following value should be added to the buffer %lu \n", time);
        //instrumentation(time);
        
    }


    public static long getCPUTime(long time){

        return System.currentTimeMillis() + time;
    }

}
