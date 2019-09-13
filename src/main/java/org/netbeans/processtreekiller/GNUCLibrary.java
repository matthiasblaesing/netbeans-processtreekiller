/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Library
 *  com.sun.jna.Native
 *  com.sun.jna.Pointer
 *  com.sun.jna.StringArray
 *  com.sun.jna.ptr.IntByReference
 *  org.jvnet.libpam.impl.CLibrary
 *  org.jvnet.libpam.impl.CLibrary$passwd
 */
package org.netbeans.processtreekiller;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public interface GNUCLibrary
extends Library {
    public static final int F_GETFD = 1;
    public static final int F_SETFD = 2;
    public static final int FD_CLOEXEC = 1;
    public static final GNUCLibrary LIBC = (GNUCLibrary)Native.loadLibrary((String)"c", GNUCLibrary.class);

    public int fork();

    public int kill(int var1, int var2);

    public int setsid();

    public int umask(int var1);

    public int getpid();

    public int geteuid();

    public int getegid();

    public int getppid();

    public int chdir(String var1);

    public int getdtablesize();

    public int execv(String var1, StringArray var2);

    public int setenv(String var1, String var2);

    public int unsetenv(String var1);

    public void perror(String var1);

    public String strerror(int var1);

    public passwd getpwuid(int var1);

    public int fcntl(int var1, int var2);

    public int fcntl(int var1, int var2, int var3);

    public int chown(String var1, int var2, int var3);

    public int chmod(String var1, int var2);

    public int sysctlbyname(String var1, Pointer var2, IntByReference var3, Pointer var4, IntByReference var5);

    public int sysctl(int[] var1, int var2, Pointer var3, IntByReference var4, Pointer var5, IntByReference var6);

    public int sysctlnametomib(String var1, Pointer var2, IntByReference var3);

    static class passwd extends Structure {
        String pw_name;       /* username */
        String pw_passwd;     /* user password */
        int   pw_uid;        /* user ID */
        int   pw_gid;        /* group ID */
        String  pw_gecos;      /* user information */
        String   pw_dir;        /* home directory */
        String   pw_shell;      /* shell program */
    };
}

