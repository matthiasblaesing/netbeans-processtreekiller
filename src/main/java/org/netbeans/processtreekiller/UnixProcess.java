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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class UnixProcess<P extends UnixProcess<P>> {
    private static final Method DESTROY_PROCESS;
    private static final Logger LOGGER = Logger.getLogger(UnixProcess.class.getName());

    static {
        Method destroyMethod;
        try {
            Class<?> clazz = Class.forName("java.lang.UNIXProcess");
            try {
                destroyMethod = clazz.getDeclaredMethod("destroyProcess", Integer.TYPE);
            } catch (NoSuchMethodException ex) {
                destroyMethod = clazz.getDeclaredMethod("destroyProcess", Integer.TYPE, Boolean.TYPE);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LinkageError x = new LinkageError();
            x.initCause(e);
            throw x;
        }
        DESTROY_PROCESS = destroyMethod;
        DESTROY_PROCESS.setAccessible(true);
    }

    public final UnixSystem<P> system;

    protected UnixProcess(UnixSystem<P> system) {
        this.system = system;
    }

    public abstract int getPid();

    public abstract P getParent();

    protected final File getFile(String relativePath) {
        return new File(new File("/proc/" + this.getPid()), relativePath);
    }

    protected final Path getPath(String relativePath) {
        return Paths.get("/proc/", Integer.toString(this.getPid()), relativePath);
    }

    public List<UnixProcess> getChildren() {
        ArrayList<UnixProcess> r = new ArrayList<>();
        for (UnixProcess p : this.system) {
            if (p.getParent() != this) {
                continue;
            }
            r.add(p);
        }
        return r;
    }

    public void kill() {
        try {
            if (DESTROY_PROCESS.getParameterTypes().length > 1) {
                DESTROY_PROCESS.invoke(null, this.getPid(), false);
            } else {
                DESTROY_PROCESS.invoke(null, this.getPid());
            }
        } catch (IllegalAccessException e) {
            IllegalAccessError x = new IllegalAccessError();
            x.initCause(e);
            throw x;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            }
            LOGGER.log(Level.INFO, "Failed to terminate pid=" + this.getPid(), e);
        }
    }

    public void killRecursively() {
        for (UnixProcess p : this.getChildren()) {
            p.killRecursively();
        }
        this.kill();
    }

    public abstract EnvVars getEnvVars();

    public abstract List<String> getArguments();
}
