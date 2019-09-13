/*
 * Decompiled with CFR 0.144.
 */
package org.netbeans.processtreekiller;

import java.util.Collection;
import java.util.Map;

public interface VariableResolver<V> {
    public static final VariableResolver NONE = new VariableResolver(){

        public Object resolve(String name) {
            return null;
        }
    };

    public V resolve(String var1);

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static final class Union<V>
    implements VariableResolver<V> {
        private final VariableResolver<? extends V>[] resolvers;

        public Union(VariableResolver<? extends V> ... resolvers) {
            this.resolvers = (VariableResolver[])resolvers.clone();
        }

        public Union(Collection<? extends VariableResolver<? extends V>> resolvers) {
            this.resolvers = resolvers.toArray(new VariableResolver[resolvers.size()]);
        }

        @Override
        public V resolve(String name) {
            for (VariableResolver<V> r : this.resolvers) {
                V v = r.resolve(name);
                if (v == null) continue;
                return v;
            }
            return null;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static final class ByMap<V>
    implements VariableResolver<V> {
        private final Map<String, V> data;

        public ByMap(Map<String, V> data) {
            this.data = data;
        }

        @Override
        public V resolve(String name) {
            return this.data.get(name);
        }
    }

}

