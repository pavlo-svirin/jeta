/*
 * Copyright 2015 Oleg Khalidov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.javameta.metasitory;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Used to search metacodes in a metasitory
 *
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public final class Criteria {
    /**
     * Metasitory implementation should check its compatibility with criteria by this constant
     * and throw IllegalArgumentException if this value is higher
     */
    public static final int VERSION = 1;

    private Class masterEq;

    private Class masterEqDeep;

    private Set<Class<? extends Annotation>> usesAny;

    private Set<Class<? extends Annotation>> usesAll;

    private Criteria() {

    }

    public static class Builder {
        Criteria criteria = new Criteria();

        public Criteria build() {
            return criteria;
        }

        public Builder masterEq(Class value) {
            criteria.masterEq = value;
            return this;
        }

        public Builder masterEqDeep(Class value) {
            criteria.masterEqDeep = value;
            return this;
        }

        public Builder usesAny(Set<Class<? extends Annotation>> value) {
            criteria.usesAny = value;
            return this;
        }

        public Builder usesAny(Class<? extends Annotation> value) {
            if (criteria.usesAny == null)
                criteria.usesAny = new HashSet<>();
            criteria.usesAny.add(value);
            return this;
        }

        public Builder usesAll(Set<Class<? extends Annotation>> value) {
            criteria.usesAll = value;
            return this;
        }
    }

    @Nullable
    public Class getMasterEq() {
        return masterEq;
    }

    @Nullable
    public Class getMasterEqDeep() {
        return masterEqDeep;
    }

    @Nullable
    public Set<Class<? extends Annotation>> getUsesAny() {
        return usesAny;
    }

    @Nullable
    public Set<Class<? extends Annotation>> getUsesAll() {
        return usesAll;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Criteria criteria = (Criteria) o;
        return Objects.equals(masterEq, criteria.masterEq)
                && Objects.equals(masterEqDeep, criteria.masterEqDeep)
                && Objects.equals(usesAll, criteria.usesAll)
                && Objects.equals(usesAny, criteria.usesAny);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterEq, masterEqDeep, usesAny, usesAll);
    }

    @Override
    public String toString() {
        return "Criteria{" +
                "masterEq=" + masterEq +
                ", masterEqDeep=" + masterEqDeep +
                ", usesAny=" + usesAny +
                ", usesAll=" + usesAll +
                '}';
    }
}


