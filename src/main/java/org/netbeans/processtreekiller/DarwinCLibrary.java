/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * Copyright (c) 2020, Matthias Bläsing
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.netbeans.processtreekiller;

import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;

public interface DarwinCLibrary extends Library {
    public static final DarwinCLibrary LIBC = Native.load("c", DarwinCLibrary.class);

    public static final int sizeOf_kinfo_proc = 648;
    public static final int CTL_KERN = 1;
    public static final int KERN_PROC = 14;
    public static final int KERN_PROC_ALL = 0;
    public static final int KERN_ARGMAX = 8;
    public static final int KERN_PROCARGS2 = 49;

    public String strerror(int var1);

    public int sysctl(int[] name, int namelen, Pointer oldp, SizeTByReference oldlenp, Pointer newp, SizeT newlen);

    class SizeTByReference extends ByReference {

        public SizeTByReference() {
            super(Native.SIZE_T_SIZE);
        }

        public long getValue() {
            if(getPointer() == null) {
                return 0;
            }
            switch(Native.SIZE_T_SIZE) {
                case 1:
                    return Byte.toUnsignedLong(getPointer().getByte(0));
                case 2:
                    return Short.toUnsignedLong(getPointer().getShort(0));
                case 4:
                    return Integer.toUnsignedLong(getPointer().getInt(0));
                case 8:
                    return getPointer().getLong(0);
            }
            throw new IllegalStateException("Unsupported SIZE_T size: " + Native.SIZE_T_SIZE);
        }

        public void setValue(long value) {
            switch (Native.SIZE_T_SIZE) {
                case 1:
                    getPointer().setByte(0, (byte) value);
                    return;
                case 2:
                    getPointer().setShort(0, (short) value);
                    return;
                case 4:
                    getPointer().setInt(0, (int) value);
                    return;
                case 8:
                    getPointer().setLong(0, (long) value);
                    return;
            }
            throw new IllegalStateException("Unsupported SIZE_T size: " + Native.SIZE_T_SIZE);
        }
    }

    class SizeT extends IntegerType {

        public SizeT() {
            this(0);
        }

        public SizeT(long value) {
            super(Native.SIZE_T_SIZE, value, true);
        }


    }
}

