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

import com.sun.jna.Platform;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ProcessTreeKiller {
    private static final ProcessTreeKiller DEFAULT = new ProcessTreeKiller(){

        @Override
        public void kill(Process proc, Map<String, String> modelEnvVars) {
            if (proc != null) {
                proc.destroy();
            }
        }
    };

    private static final Logger LOGGER = Logger.getLogger(ProcessTreeKiller.class.getName());
    public static boolean enabled = !Boolean.getBoolean(ProcessTreeKiller.class.getName() + ".disable");

    public void kill(Process proc) {
        kill(proc, null);
    }

    public abstract void kill(Process var1, Map<String, String> var2);

    public void kill(Map<String, String> modelEnvVars) {
        kill(null, modelEnvVars);
    }

    public static ProcessTreeKiller get() {
        if (!enabled) {
            return DEFAULT;
        }
        try {
            if (Platform.isWindows()) {
                return new Windows();
            } else if (Platform.isLinux()) {
                return new Linux();
            } else if (Platform.isSolaris()) {
                return new Solaris();
            } else if (Platform.isMac()) {
                return new Darwin();
            }
        }
        catch (LinkageError e) {
            LOGGER.log(Level.WARNING, "Failed to load winp. Reverting to the default", e);
            enabled = false;
        }
        return DEFAULT;
    }

    protected boolean hasMatchingEnvVars(Map<String, String> envVar, Map<String, String> modelEnvVar) {
        if (modelEnvVar.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> e : modelEnvVar.entrySet()) {
            String v = envVar.get(e.getKey());
            if (v != null && v.equals(e.getValue())) continue;
            return false;
        }
        return true;
    }

}

