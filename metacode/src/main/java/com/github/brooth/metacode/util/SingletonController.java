package com.github.brooth.metacode.util;

import com.github.brooth.metacode.MasterClassController;
import com.github.brooth.metacode.MasterMetacode;
import com.github.brooth.metacode.metasitory.ClassForNameMetasitory;
import com.github.brooth.metacode.metasitory.Criteria;
import com.github.brooth.metacode.metasitory.Metasitory;
import com.google.common.collect.Iterables;

/**
 *
 */
public class SingletonController<M> extends MasterClassController<M, MasterMetacode> {

    public SingletonController(Class<? extends M> masterClass) {
        this(new ClassForNameMetasitory(), masterClass);
    }

    public SingletonController(Metasitory metasitory, Class<? extends M> masterClass) {
        super(metasitory, masterClass);
    }

    @Override
    protected Criteria criteria() {
        return new Criteria.Builder().masterEq(masterClass).build();
    }

    public M getInstance() {
        MasterMetacode singleton = Iterables.getFirst(metacodes, null);
        if (singleton == null || !(singleton instanceof SingletonMetacode))
            throw new IllegalStateException(masterClass.getCanonicalName() + " has not singleton meta code. No @Singleton annotation on it?");

        @SuppressWarnings("unchecked")
        M instance = (M) ((SingletonMetacode) singleton).getInstance();
        return instance;
    }

}
