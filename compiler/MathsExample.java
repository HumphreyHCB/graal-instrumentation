/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

public class MathsExample {
    public static void main(String[] args) {
        for (int i = 0; i < 1_000_000; i++) {
                System.out.println(add(i));
        }
                
        }
public static void mathstest(){
        for (int i = 0; i < 1_000_000; i++) {
                System.out.println(add(i));
        }

}

        public static int add(int number) {
               return  100 + number;
        }

        public int mandelbrot(final int size) {
                int sum     = 0;
                int byteAcc = 0;
                int bitNum  = 0;
           
                int y = 0;
           
                while (y < size) {
                  double ci = (2.0 * y / size) - 1.0;
                  int x = 0;
           
                  while (x < size) {
                    double zrzr = 0.0;
                    double zi   = 0.0;
                    double zizi = 0.0;
                    double cr = (2.0 * x / size) - 1.5;
           
                    int z = 0;
                    boolean notDone = true;
                    int escape = 0;
                    while (notDone && z < 50) {
                      double zr = zrzr - zizi + cr;
                      zi = 2.0 * zr * zi + ci;
           
                      // preserve recalculation
                      zrzr = zr * zr;
                      zizi = zi * zi;
           
                      if (zrzr + zizi > 4.0) {
                        notDone = false;
                        escape  = 1;
                      }
                      z += 1;
                    }
           
                    byteAcc = (byteAcc << 1) + escape;
                    bitNum += 1;
           
                    // Code is very similar for these cases, but using separate blocks
                    // ensures we skip the shifting when it's unnecessary, which is most cases.
                    if (bitNum == 8) {
                      sum ^= byteAcc;
                      byteAcc = 0;
                      bitNum  = 0;
                    } else if (x == size - 1) {
                      byteAcc <<= (8 - bitNum);
                      sum ^= byteAcc;
                      byteAcc = 0;
                      bitNum  = 0;
                    }
                    x += 1;
                  }
                  y += 1;
                }
                return sum;
              }
}
