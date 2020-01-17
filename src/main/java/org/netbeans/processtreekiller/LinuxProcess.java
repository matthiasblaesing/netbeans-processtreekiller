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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class LinuxProcess extends UnixProcess<LinuxProcess> {

    private final int pid;
    private int ppid = -1;
    private EnvVars envVars;
    private List<String> arguments;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    LinuxProcess(LinuxSystem system, int pid) throws IOException {
        super(system);
        this.pid = pid;
        try (BufferedReader r = new BufferedReader(new FileReader(this.getFile("status")))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!(line = line.toLowerCase(Locale.ENGLISH)).startsWith("ppid:")) {
                    continue;
                }
                this.ppid = Integer.parseInt(line.substring(5).trim());
                break;
            }
        }
        if (this.ppid == -1) {
            throw new IOException("Failed to parse PPID from /proc/" + pid + "/status");
        }
    }

    @Override
    public int getPid() {
        return this.pid;
    }

    @Override
    public LinuxProcess getParent() {
        return (LinuxProcess) this.system.get(this.ppid);
    }

    @Override
    public synchronized List<String> getArguments() {
        if (this.arguments != null) {
            return this.arguments;
        }
        this.arguments = new ArrayList<>();
        try {
            byte[] cmdline = Files.readAllBytes(this.getPath("cmdline"));
            int pos = 0;
            for (int i = 0; i < cmdline.length; ++i) {
                byte b = cmdline[i];
                if (b != 0) {
                    continue;
                }
                this.arguments.add(new String(cmdline, pos, i - pos));
                pos = i + 1;
            }
        } catch (IOException cmdline) {
            // empty catch block
        }
        this.arguments = Collections.unmodifiableList(this.arguments);
        return this.arguments;
    }

    @Override
    public synchronized EnvVars getEnvVars() {
        if (this.envVars != null) {
            return this.envVars;
        }
        this.envVars = new EnvVars();
        try {
            byte[] environ = Files.readAllBytes(this.getPath("environ"));
            int pos = 0;
            for (int i = 0; i < environ.length; ++i) {
                byte b = environ[i];
                if (b != 0) {
                    continue;
                }
                this.envVars.addLine(new String(environ, pos, i - pos));
                pos = i + 1;
            }
        } catch (IOException environ) {
            // empty catch block
        }
        return this.envVars;
    }
}
