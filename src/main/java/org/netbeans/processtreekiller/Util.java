/*
 * Decompiled with CFR 0.144.
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

