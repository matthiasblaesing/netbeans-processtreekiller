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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.netbeans.processtreekiller.DarwinCLibrary.CTL_KERN;
import static org.netbeans.processtreekiller.DarwinCLibrary.KERN_ARGMAX;
import static org.netbeans.processtreekiller.DarwinCLibrary.KERN_PROCARGS2;

class DarwinProcess extends UnixProcess<DarwinProcess> {

    private final int pid;
    private final int ppid;
    private EnvVars envVars;
    private List<String> arguments;

    DarwinProcess(DarwinSystem system, int pid, int ppid) {
        super(system);
        this.pid = pid;
        this.ppid = ppid;
    }

    @Override
    public int getPid() {
        return this.pid;
    }

    @Override
    public DarwinProcess getParent() {
        return (DarwinProcess) this.system.get(this.ppid);
    }

    @Override
    public synchronized EnvVars getEnvVars() {
        if (this.envVars != null) {
            return this.envVars;
        }
        this.parse();
        return this.envVars;
    }

    @Override
    public List<String> getArguments() {
        if (this.arguments != null) {
            return this.arguments;
        }
        this.parse();
        return this.arguments;
    }

    private void parse() {
        try {
            this.arguments = new ArrayList<>();
            this.envVars = new EnvVars();
            DarwinCLibrary.SizeT newLen = new DarwinCLibrary.SizeT();
            IntByReference argmaxRef = new IntByReference(0);
            DarwinCLibrary.SizeTByReference size = new DarwinCLibrary.SizeTByReference();
            size.setValue(4 /*
             * sizeof(int)
             */);
            if (DarwinCLibrary.LIBC.sysctl(new int[]{CTL_KERN, KERN_ARGMAX}, 2, argmaxRef.getPointer(), size, null, newLen) != 0) {
                throw new IOException("Failed to get kernl.argmax: " + DarwinCLibrary.LIBC.strerror(Native.getLastError()));
            }
            int argmax = argmaxRef.getValue();
            class StringArrayMemory extends Memory {

                private long offset;

                StringArrayMemory(long l) {
                    super(l);
                    this.offset = 0L;
                }

                int readInt() {
                    int r = this.getInt(this.offset);
                    this.offset += 4;
                    return r;
                }

                byte peek() {
                    return this.getByte(this.offset);
                }

                String readString() {
                    byte ch;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((ch = this.getByte(this.offset++)) != 0 && this.offset < size()) {
                        baos.write(ch);
                    }
                    return Native.toString(baos.toByteArray());
                }

                void skip0() {
                    while (this.getByte(this.offset) == 0 && this.offset < size()) {
                        ++this.offset;
                    }
                }
            }
            StringArrayMemory m = new StringArrayMemory(argmax);
            size.setValue(argmax);
            if (DarwinCLibrary.LIBC.sysctl(new int[]{CTL_KERN, KERN_PROCARGS2, this.pid}, 3, m, size, null, newLen) != 0) {
                throw new IOException("Failed to obtain ken.procargs2: " + DarwinCLibrary.LIBC.strerror(Native.getLastError()));
            }
            int argc = m.readInt();
            String args0 = m.readString();
            m.skip0();
            try {
                for (int i = 0; i < argc; ++i) {
                    this.arguments.add(m.readString());
                }
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalStateException("Failed to parse arguments: arg0=" + args0 + ", arguments=" + this.arguments + ", nargs=" + argc, e);
            }
            while (m.peek() != 0) {
                this.envVars.addLine(m.readString());
            }
        } catch (IOException igored) {
            // empty catch block
        }
    }

}
