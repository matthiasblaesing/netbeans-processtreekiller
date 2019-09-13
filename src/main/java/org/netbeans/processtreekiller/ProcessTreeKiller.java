/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Memory
 *  com.sun.jna.Native
 *  com.sun.jna.Pointer
 *  com.sun.jna.ptr.IntByReference
 *  org.apache.commons.io.FileUtils
 *  org.jvnet.winp.WinProcess
 *  org.jvnet.winp.WinpException
 */
package org.netbeans.processtreekiller;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jvnet.winp.WinProcess;
import org.jvnet.winp.WinpException;
import org.netbeans.processtreekiller.EnvVars;
import org.netbeans.processtreekiller.GNUCLibrary;
import org.netbeans.processtreekiller.Util;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class ProcessTreeKiller {
    private static final ProcessTreeKiller DEFAULT = new ProcessTreeKiller(){

        @Override
        public void kill(Process proc, Map<String, String> modelEnvVars) {
            if (proc != null) {
                proc.destroy();
            }
        }
    };
    private static final boolean IS_LITTLE_ENDIAN = "little".equals(System.getProperty("sun.cpu.endian"));
    private static final Logger LOGGER = Logger.getLogger(ProcessTreeKiller.class.getName());
    public static boolean enabled = !Boolean.getBoolean(ProcessTreeKiller.class.getName() + ".disable");

    public void kill(Process proc) {
        this.kill(proc, null);
    }

    public abstract void kill(Process var1, Map<String, String> var2);

    public void kill(Map<String, String> modelEnvVars) {
        this.kill(null, modelEnvVars);
    }

    public static EnvVars createCookie() {
        return new EnvVars("HUDSON_COOKIE", UUID.randomUUID().toString());
    }

    public static ProcessTreeKiller get() {
        if (!enabled) {
            return DEFAULT;
        }
        try {
            if (File.pathSeparatorChar == ';') {
                return new Windows();
            }
            String os = Util.fixNull(System.getProperty("os.name"));
            if (os.equals("Linux")) {
                return new Linux();
            }
            if (os.equals("SunOS")) {
                return new Solaris();
            }
            if (os.equals("Mac OS X")) {
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

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class Darwin
    extends Unix<DarwinSystem> {
        private static final int sizeOf_kinfo_proc = 648;
        private static final int CTL_KERN = 1;
        private static final int KERN_PROC = 14;
        private static final int KERN_PROC_ALL = 0;
        private static final int KERN_ARGMAX = 8;
        private static final int KERN_PROCARGS2 = 49;
        private static final int ENOMEM = 12;
        private static final int sizeOfInt = Native.getNativeSize(Integer.TYPE);
        private static int[] MIB_PROC_ALL = new int[]{1, 14, 0};

        private Darwin() {
            super();
        }

        @Override
        protected DarwinSystem createSystem() {
            return new DarwinSystem();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class DarwinProcess
        extends Unix.UnixProcess<DarwinProcess> {
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
                return (DarwinProcess)this.system.get(this.ppid);
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
                    this.arguments = new ArrayList<String>();
                    this.envVars = new EnvVars();
                    IntByReference _ = new IntByReference();
                    IntByReference argmaxRef = new IntByReference(0);
                    IntByReference size = new IntByReference(sizeOfInt);
                    if (GNUCLibrary.LIBC.sysctl(new int[]{1, 8}, 2, argmaxRef.getPointer(), size, Pointer.NULL, _) != 0) {
                        throw new IOException("Failed to get kernl.argmax: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                    }
                    int argmax = argmaxRef.getValue();
                    class StringArrayMemory
                    extends Memory {
                        private long offset;

                        StringArrayMemory(long l) {
                            super(l);
                            this.offset = 0L;
                        }

                        int readInt() {
                            int r = this.getInt(this.offset);
                            this.offset += (long)sizeOfInt;
                            return r;
                        }

                        byte peek() {
                            return this.getByte(this.offset);
                        }

                        String readString() {
                            byte ch;
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            while ((ch = this.getByte(this.offset++)) != 0) {
                                baos.write(ch);
                            }
                            return baos.toString();
                        }

                        void skip0() {
                            while (this.getByte(this.offset) == 0) {
                                ++this.offset;
                            }
                        }
                    }
                    StringArrayMemory m = new StringArrayMemory(argmax);
                    size.setValue(argmax);
                    if (GNUCLibrary.LIBC.sysctl(new int[]{1, 49, this.pid}, 3, (Pointer)m, size, Pointer.NULL, _) != 0) {
                        throw new IOException("Failed to obtain ken.procargs2: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                    }
                    int argc = m.readInt();
                    String args0 = m.readString();
                    m.skip0();
                    try {
                        for (int i = 0; i < argc; ++i) {
                            this.arguments.add(m.readString());
                        }
                    }
                    catch (IndexOutOfBoundsException e) {
                        throw new IllegalStateException("Failed to parse arguments: arg0=" + args0 + ", arguments=" + this.arguments + ", nargs=" + argc, e);
                    }
                    while (m.peek() != 0) {
                        this.envVars.addLine(m.readString());
                    }
                }
                catch (IOException _) {
                    // empty catch block
                }
            }

        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class DarwinSystem
        extends Unix.UnixSystem<DarwinProcess> {
            DarwinSystem() {
                try {
                    Memory m;
                    IntByReference size;
                    block5 : {
                        IntByReference _ = new IntByReference(sizeOfInt);
                        size = new IntByReference(sizeOfInt);
                        int nRetry = 0;
                        do {
                            if (GNUCLibrary.LIBC.sysctl(MIB_PROC_ALL, 3, Pointer.NULL, size, Pointer.NULL, _) != 0) {
                                throw new IOException("Failed to obtain memory requirement: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                            }
                            m = new Memory((long)size.getValue());
                            if (GNUCLibrary.LIBC.sysctl(MIB_PROC_ALL, 3, (Pointer)m, size, Pointer.NULL, _) == 0) break block5;
                        } while (Native.getLastError() == 12 && nRetry++ < 16);
                        throw new IOException("Failed to call kern.proc.all: " + GNUCLibrary.LIBC.strerror(Native.getLastError()));
                    }
                    int count = size.getValue() / 648;
                    LOGGER.fine("Found " + count + " processes");
                    for (int base = 0; base < size.getValue(); base += 648) {
                        int pid = m.getInt((long)(base + 40));
                        int ppid = m.getInt((long)(base + 560));
                        this.processes.put(pid, new DarwinProcess(this, pid, ppid));
                    }
                }
                catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to obtain process list", e);
                }
            }
        }

    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class Solaris
    extends Unix<SolarisSystem> {
        private Solaris() {
            super();
        }

        @Override
        protected SolarisSystem createSystem() {
            return new SolarisSystem();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class SolarisProcess
        extends Unix.UnixProcess<SolarisProcess> {
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
                RandomAccessFile psinfo = new RandomAccessFile(this.getFile("psinfo"), "r");
                try {
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
                finally {
                    psinfo.close();
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
                return (SolarisProcess)this.system.get(this.ppid);
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public synchronized List<String> getArguments() {
                if (this.arguments != null) {
                    return this.arguments;
                }
                this.arguments = new ArrayList<String>(this.argc);
                try {
                    RandomAccessFile as = new RandomAccessFile(this.getFile("as"), "r");
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer("Reading " + this.getFile("as"));
                    }
                    try {
                        for (int n = 0; n < this.argc; ++n) {
                            as.seek(SolarisProcess.to64(this.argp + n * 4));
                            int p = this.adjust(as.readInt());
                            this.arguments.add(this.readLine(as, p, "argv[" + n + "]"));
                        }
                    }
                    finally {
                        as.close();
                    }
                }
                catch (IOException as) {
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
                        LOGGER.finer("Reading " + this.getFile("as"));
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
                    }
                    finally {
                        as.close();
                    }
                }
                catch (IOException as) {
                    // empty catch block
                }
                return this.envVars;
            }

            private String readLine(RandomAccessFile as, int p, String prefix) throws IOException {
                int ch;
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("Reading " + prefix + " at " + p);
                }
                as.seek(SolarisProcess.to64(p));
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int i = 0;
                while ((ch = as.read()) > 0) {
                    if (++i % 100 == 0 && LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest(prefix + " is so far " + buf.toString());
                    }
                    buf.write(ch);
                }
                String line = buf.toString();
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest(prefix + " was " + line);
                }
                return line;
            }

            private static long to64(int i) {
                return (long)i & 0xFFFFFFFFL;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class SolarisSystem
        extends Unix.ProcfsUnixSystem<SolarisProcess> {
            SolarisSystem() {
            }

            @Override
            protected SolarisProcess createProcess(int pid) throws IOException {
                return new SolarisProcess(this, pid);
            }
        }

    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class Linux
    extends Unix<LinuxSystem> {
        private Linux() {
            super();
        }

        @Override
        protected LinuxSystem createSystem() {
            return new LinuxSystem();
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class LinuxProcess
        extends Unix.UnixProcess<LinuxProcess> {
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
                BufferedReader r = new BufferedReader(new FileReader(this.getFile("status")));
                try {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (!(line = line.toLowerCase(Locale.ENGLISH)).startsWith("ppid:")) continue;
                        this.ppid = Integer.parseInt(line.substring(5).trim());
                        break;
                    }
                }
                finally {
                    r.close();
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
                return (LinuxProcess)this.system.get(this.ppid);
            }

            @Override
            public synchronized List<String> getArguments() {
                if (this.arguments != null) {
                    return this.arguments;
                }
                this.arguments = new ArrayList<String>();
                try {
                    byte[] cmdline = FileUtils.readFileToByteArray((File)this.getFile("cmdline"));
                    int pos = 0;
                    for (int i = 0; i < cmdline.length; ++i) {
                        byte b = cmdline[i];
                        if (b != 0) continue;
                        this.arguments.add(new String(cmdline, pos, i - pos));
                        pos = i + 1;
                    }
                }
                catch (IOException cmdline) {
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
                    byte[] environ = FileUtils.readFileToByteArray((File)this.getFile("environ"));
                    int pos = 0;
                    for (int i = 0; i < environ.length; ++i) {
                        byte b = environ[i];
                        if (b != 0) continue;
                        this.envVars.addLine(new String(environ, pos, i - pos));
                        pos = i + 1;
                    }
                }
                catch (IOException environ) {
                    // empty catch block
                }
                return this.envVars;
            }
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static class LinuxSystem
        extends Unix.ProcfsUnixSystem<LinuxProcess> {
            LinuxSystem() {
            }

            @Override
            protected LinuxProcess createProcess(int pid) throws IOException {
                return new LinuxProcess(this, pid);
            }
        }

    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static abstract class Unix<S extends UnixSystem<?>>
    extends ProcessTreeKiller {
        private static final Field PID_FIELD;
        private static final Method DESTROY_PROCESS;

        private Unix() {
        }

        protected abstract S createSystem();

        @Override
        public void kill(Process proc, Map<String, String> modelEnvVars) {
            S system = this.createSystem();
            if (proc != null) {
                Iterator<P> p;
                try {
                    p = ((UnixSystem)system).get((Integer)PID_FIELD.get(proc));
                }
                catch (IllegalAccessException e) {
                    IllegalAccessError x = new IllegalAccessError();
                    x.initCause(e);
                    throw x;
                }
                if (p == null) {
                    proc.destroy();
                } else {
                    ((UnixProcess)((Object)p)).killRecursively();
                    proc.destroy();
                }
            }
            if (modelEnvVars != null) {
                for (UnixProcess lp : system) {
                    if (!this.hasMatchingEnvVars(lp.getEnvVars(), modelEnvVars)) continue;
                    lp.killRecursively();
                }
            }
        }

        static {
            Method destroyMethod;
            Field pidField;
            try {
                Class<?> clazz = Class.forName("java.lang.UNIXProcess");
                pidField = clazz.getDeclaredField("pid");
                try {
                    destroyMethod = clazz.getDeclaredMethod("destroyProcess", Integer.TYPE);
                }
                catch (NoSuchMethodException ex) {
                    destroyMethod = clazz.getDeclaredMethod("destroyProcess", Integer.TYPE, Boolean.TYPE);
                }
            }
            catch (ClassNotFoundException e) {
                LinkageError x = new LinkageError();
                x.initCause(e);
                throw x;
            }
            catch (NoSuchFieldException e) {
                LinkageError x = new LinkageError();
                x.initCause(e);
                throw x;
            }
            catch (NoSuchMethodException e) {
                LinkageError x = new LinkageError();
                x.initCause(e);
                throw x;
            }
            PID_FIELD = pidField;
            PID_FIELD.setAccessible(true);
            DESTROY_PROCESS = destroyMethod;
            DESTROY_PROCESS.setAccessible(true);
        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        public static abstract class UnixProcess<P extends UnixProcess<P>> {
            public final UnixSystem<P> system;

            protected UnixProcess(UnixSystem<P> system) {
                this.system = system;
            }

            public abstract int getPid();

            public abstract P getParent();

            protected final File getFile(String relativePath) {
                return new File(new File("/proc/" + this.getPid()), relativePath);
            }

            public List<P> getChildren() {
                ArrayList<UnixProcess> r = new ArrayList<UnixProcess>();
                for (UnixProcess p : this.system) {
                    if (p.getParent() != this) continue;
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
                }
                catch (IllegalAccessException e) {
                    IllegalAccessError x = new IllegalAccessError();
                    x.initCause(e);
                    throw x;
                }
                catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof Error) {
                        throw (Error)e.getTargetException();
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

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static abstract class ProcfsUnixSystem<P extends UnixProcess<P>>
        extends UnixSystem<P> {
            ProcfsUnixSystem() {
                File[] processes = new File("/proc").listFiles(new FileFilter(){

                    public boolean accept(File f) {
                        return f.isDirectory();
                    }
                });
                if (processes == null) {
                    LOGGER.info("No /proc");
                    return;
                }
                for (File p : processes) {
                    int pid;
                    try {
                        pid = Integer.parseInt(p.getName());
                    }
                    catch (NumberFormatException e) {
                        continue;
                    }
                    try {
                        this.processes.put(pid, this.createProcess(pid));
                    }
                    catch (IOException e) {
                        // empty catch block
                    }
                }
            }

            protected abstract P createProcess(int var1) throws IOException;

        }

        /*
         * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
         */
        static abstract class UnixSystem<P extends UnixProcess<P>>
        implements Iterable<P> {
            protected final Map<Integer, P> processes = new HashMap<Integer, P>();

            UnixSystem() {
            }

            public P get(int pid) {
                return (P)((UnixProcess)this.processes.get(pid));
            }

            @Override
            public Iterator<P> iterator() {
                return this.processes.values().iterator();
            }
        }

    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class Windows
    extends ProcessTreeKiller {
        private Windows() {
        }

        @Override
        public void kill(Process proc, Map<String, String> modelEnvVars) {
            if (proc != null) {
                new WinProcess(proc).killRecursively();
            }
            if (modelEnvVars != null) {
                for (WinProcess p : WinProcess.all()) {
                    boolean matched;
                    if (p.getPid() < 10) continue;
                    try {
                        matched = this.hasMatchingEnvVars(p.getEnvironmentVariables(), modelEnvVars);
                    }
                    catch (WinpException e) {
                        continue;
                    }
                    if (!matched) continue;
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

}
