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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class SolarisProcess extends UnixProcess<SolarisProcess> {

    private static final Logger LOGGER = Logger.getLogger(SolarisProcess.class.getName());
    private static final boolean IS_LITTLE_ENDIAN = "little".equals(System.getProperty("sun.cpu.endian"));

    private final int pid;
    private final int ppid;
    private final int envp;
    private final int argp;
    private final int argc;
    private EnvVars envVars;
    private List<String> arguments;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    SolarisProcess(SolarisSystem system, int pid) throws IOException {
        super(system);
        this.pid = pid;
        try (RandomAccessFile psinfo = new RandomAccessFile(this.getFile("psinfo"), "r")) {
            psinfo.seek(8L);
            if (this.adjust(psinfo.readInt()) != pid) {
                throw new IOException("psinfo PID mismatch");
            }
            this.ppid = this.adjust(psinfo.readInt());
            psinfo.seek(188L);
            this.argc = this.adjust(psinfo.readInt());
            this.argp = this.adjust(psinfo.readInt());
            this.envp = this.adjust(psinfo.readInt());
        }
        if (this.ppid == -1) {
            throw new IOException("Failed to parse PPID from /proc/" + pid + "/status");
        }
    }

    private int adjust(int i) {
        if (IS_LITTLE_ENDIAN) {
            return i << 24 | i << 8 & 16711680 | i >> 8 & 65280 | i >>> 24;
        }
        return i;
    }

    @Override
    public int getPid() {
        return this.pid;
    }

    @Override
    public SolarisProcess getParent() {
        return (SolarisProcess) this.system.get(this.ppid);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public synchronized List<String> getArguments() {
        if (this.arguments != null) {
            return this.arguments;
        }
        this.arguments = new ArrayList<>(this.argc);
        try {
            RandomAccessFile as = new RandomAccessFile(this.getFile("as"), "r");
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "Reading {0}", this.getFile("as"));
            }
            try {
                for (int n = 0; n < this.argc; ++n) {
                    as.seek(SolarisProcess.to64(this.argp + n * 4));
                    int p = this.adjust(as.readInt());
                    this.arguments.add(this.readLine(as, p, "argv[" + n + "]"));
                }
            } finally {
                as.close();
            }
        } catch (IOException as) {
            // empty catch block
        }
        this.arguments = Collections.unmodifiableList(this.arguments);
        return this.arguments;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public synchronized EnvVars getEnvVars() {
        if (this.envVars != null) {
            return this.envVars;
        }
        this.envVars = new EnvVars();
        try {
            RandomAccessFile as = new RandomAccessFile(this.getFile("as"), "r");
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "Reading {0}", this.getFile("as"));
            }
            try {
                int n = 0;
                do {
                    as.seek(SolarisProcess.to64(this.envp + n * 4));
                    int p = this.adjust(as.readInt());
                    if (p == 0) {
                        break;
                    }
                    this.envVars.addLine(this.readLine(as, p, "env[" + n + "]"));
                    ++n;
                } while (true);
            } finally {
                as.close();
            }
        } catch (IOException as) {
            // empty catch block
        }
        return this.envVars;
    }

    private String readLine(RandomAccessFile as, int p, String prefix) throws IOException {
        int ch;
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Reading {0} at {1}", new Object[]{prefix, p});
        }
        as.seek(SolarisProcess.to64(p));
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int i = 0;
        while ((ch = as.read()) > 0) {
            if (++i % 100 == 0 && LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "{0} is so far {1}", new Object[]{prefix, buf.toString()});
            }
            buf.write(ch);
        }
        String line = buf.toString();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "{0} was {1}", new Object[]{prefix, line});
        }
        return line;
    }

    private static long to64(int i) {
        return (long) i & 0xFFFFFFFFL;
    }
}
