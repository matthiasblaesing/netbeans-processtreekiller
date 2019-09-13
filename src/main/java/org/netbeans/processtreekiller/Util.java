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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.processtreekiller.VariableResolver;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Util {
    private static final Pattern VARIABLE = Pattern.compile("\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_]+\\}|\\$)");

    public static String replaceMacro(String s, Map<String, String> properties) {
        return Util.replaceMacro(s, new VariableResolver.ByMap<String>(properties));
    }

    public static String replaceMacro(String s, VariableResolver<String> resolver) {
        if (s == null) {
            return null;
        }
        int idx = 0;
        Matcher m;
        while ((m = VARIABLE.matcher(s)).find(idx)) {
            String value;
            String key = m.group().substring(1);
            if (key.charAt(0) == '$') {
                value = "$";
            } else {
                if (key.charAt(0) == '{') {
                    key = key.substring(1, key.length() - 1);
                }
                value = resolver.resolve(key);
            }
            if (value == null) {
                idx = m.end();
                continue;
            }
            s = s.substring(0, m.start()) + value + s.substring(m.end());
            idx = m.start() + value.length();
        }
        return s;
    }

    public static String fixNull(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }
}

