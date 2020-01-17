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

import java.util.Map;
import org.jvnet.winp.WinProcess;
import org.jvnet.winp.WinpException;

final class Windows extends ProcessTreeKiller {

    Windows() {
    }

    @Override
    public void kill(Process proc, Map<String, String> modelEnvVars) {
        if (proc != null) {
            new WinProcess(proc).killRecursively();
        }
        if (modelEnvVars != null) {
            for (WinProcess p : WinProcess.all()) {
                boolean matched;
                if (p.getPid() < 10) {
                    continue;
                }
                try {
                    matched = this.hasMatchingEnvVars(p.getEnvironmentVariables(), modelEnvVars);
                } catch (WinpException e) {
                    continue;
                }
                if (!matched) {
                    continue;
                }
                p.killRecursively();
            }
        }
    }

    static {
        if (System.getProperty("winp.folder.preferred") == null) {
            String userhome = System.getProperty("netbeans.user");
            System.setProperty("winp.folder.preferred", userhome);
        }
        WinProcess.enableDebugPrivilege();
    }
}
