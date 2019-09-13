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

import java.util.Collection;
import java.util.Map;

public interface VariableResolver<V> {
    @SuppressWarnings("Convert2Lambda")
    public static final VariableResolver NONE = new VariableResolver(){

        @Override
        public Object resolve(String name) {
            return null;
        }
    };

    public V resolve(String var1);

    public static final class Union<V> implements VariableResolver<V> {
        private final VariableResolver<? extends V>[] resolvers;

        public Union(VariableResolver<? extends V> ... resolvers) {
            this.resolvers = (VariableResolver[])resolvers.clone();
        }

        public Union(Collection<? extends VariableResolver<? extends V>> resolvers) {
            this.resolvers = resolvers.toArray(new VariableResolver[0]);
        }

        @Override
        public V resolve(String name) {
            for (VariableResolver<? extends V> r : this.resolvers) {
                V v = r.resolve(name);
                if (v != null) {
                    return v;
                }
            }
            return null;
        }
    }

    public static final class ByMap<V> implements VariableResolver<V> {

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

