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

import java.lang.reflect.Field;
import java.util.Map;

abstract class Unix<S extends UnixSystem<?>> extends ProcessTreeKiller {

    private static final Field PID_FIELD;

    Unix() {
    }

    protected abstract S createSystem();

    @Override
    public void kill(Process proc, Map<String, String> modelEnvVars) {
        S system = this.createSystem();
        if (proc != null) {
            UnixProcess p;
            try {
                p = ((UnixSystem) system).get((Integer) PID_FIELD.get(proc));
            } catch (IllegalAccessException e) {
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(e);
                throw x;
            }
            if (p == null) {
                proc.destroy();
            } else {
                ((UnixProcess) ((Object) p)).killRecursively();
                proc.destroy();
            }
        }
        if (modelEnvVars != null) {
            for (UnixProcess lp : system) {
                if (!this.hasMatchingEnvVars(lp.getEnvVars(), modelEnvVars)) {
                    continue;
                }
                lp.killRecursively();
            }
        }
    }

    static {
        Field pidField;
        try {
            Class<?> clazz = Class.forName("java.lang.UNIXProcess");
            pidField = clazz.getDeclaredField("pid");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            LinkageError x = new LinkageError();
            x.initCause(e);
            throw x;
        }
        PID_FIELD = pidField;
        PID_FIELD.setAccessible(true);
    }

}
