/*
 * Decompiled with CFR 0.144.
 */
package org.netbeans.processtreekiller;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.netbeans.processtreekiller.Util;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class EnvVars
extends TreeMap<String, String> {
    public static final Map<String, String> masterEnvVars = EnvVars.initMaster();

    public EnvVars() {
        super(String.CASE_INSENSITIVE_ORDER);
    }

    public EnvVars(Map<String, String> m) {
        this();
        this.putAll(m);
        if (m instanceof EnvVars) {
            EnvVars envVars = (EnvVars)m;
        }
    }

    public EnvVars(EnvVars m) {
        this((Map<String, String>)m);
    }

    public EnvVars(String ... keyValuePairs) {
        this();
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(Arrays.asList(keyValuePairs).toString());
        }
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            this.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
    }

    public void override(String key, String value) {
        if (value == null || value.length() == 0) {
            this.remove(key);
            return;
        }
        int idx = key.indexOf(43);
        if (idx > 0) {
            String realKey = key.substring(0, idx);
            String v = (String)this.get(realKey);
            if (v == null) {
                v = value;
            } else {
                char ch = File.pathSeparatorChar;
                v = value + ch + v;
            }
            this.put(realKey, v);
            return;
        }
        this.put(key, value);
    }

    public EnvVars overrideAll(Map<String, String> all) {
        for (Map.Entry<String, String> e : all.entrySet()) {
            this.override(e.getKey(), e.getValue());
        }
        return this;
    }

    public static void resolve(Map<String, String> env) {
        for (Map.Entry<String, String> entry : env.entrySet()) {
            entry.setValue(Util.replaceMacro(entry.getValue(), env));
        }
    }

    public void addLine(String line) {
        int sep = line.indexOf(61);
        if (sep > 0) {
            this.put(line.substring(0, sep), line.substring(sep + 1));
        }
    }

    public String expand(String s) {
        return Util.replaceMacro(s, this);
    }

    private static EnvVars initMaster() {
        EnvVars vars = new EnvVars(System.getenv());
        return vars;
    }
}

