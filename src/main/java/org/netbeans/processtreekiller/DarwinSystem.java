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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.netbeans.processtreekiller.DarwinCLibrary.CTL_KERN;
import static org.netbeans.processtreekiller.DarwinCLibrary.KERN_PROC;
import static org.netbeans.processtreekiller.DarwinCLibrary.KERN_PROC_ALL;
import static org.netbeans.processtreekiller.DarwinCLibrary.sizeOf_kinfo_proc;

class DarwinSystem extends UnixSystem<DarwinProcess> {

    private static final Logger LOGGER = Logger.getLogger(DarwinSystem.class.getName());

    private static final int[] MIB_PROC_ALL = new int[]{CTL_KERN, KERN_PROC, KERN_PROC_ALL};

    DarwinSystem() {
        try {
            Memory m;
            DarwinCLibrary.SizeTByReference size;
            OUTER:
            {
                DarwinCLibrary.SizeT newLen = new DarwinCLibrary.SizeT();
                size = new DarwinCLibrary.SizeTByReference();
                int nRetry = 0;
                do {
                    if (DarwinCLibrary.LIBC.sysctl(MIB_PROC_ALL, 3, null, size, null, newLen) != 0) {
                        throw new IOException("Failed to obtain memory requirement: " + DarwinCLibrary.LIBC.strerror(Native.getLastError()));
                    }
                    m = new Memory(size.getValue());
                    if (DarwinCLibrary.LIBC.sysctl(MIB_PROC_ALL, 3, m, size, null, newLen) == 0) {
                        break OUTER;
                    }
                } while (Native.getLastError() == 12 && nRetry++ < 16);
                throw new IOException("Failed to call kern.proc.all: " + DarwinCLibrary.LIBC.strerror(Native.getLastError()));
            }
            int count = (int) (size.getValue() / sizeOf_kinfo_proc);
            LOGGER.log(Level.FINE, "Found {0} processes", count);
            for (int base = 0; base < size.getValue(); base += sizeOf_kinfo_proc) {
                int pid = m.getInt((long) (base + 40));
                int ppid = m.getInt((long) (base + 560));
                this.processes.put(pid, new DarwinProcess(this, pid, ppid));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to obtain process list", e);
        }
    }
}
