/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public interface GNUCLibrary extends Library {
    public static final int F_GETFD = 1;
    public static final int F_SETFD = 2;
    public static final int FD_CLOEXEC = 1;
    public static final GNUCLibrary LIBC = Native.load((String)"c", GNUCLibrary.class);

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

