package com.github.brooth.metacode.apt;

import com.github.brooth.metacode.util.Singleton;
import com.squareup.javapoet.TypeSpec;

/**
 * 
 */
public class SingletonProcessor extends SimpleProcessor {

    public SingletonProcessor() {
        super(Singleton.class);
    }

    @Override
    public boolean process(ProcessorContext ctx, TypeSpec masterType, int round) {
        return false;
    }
}
