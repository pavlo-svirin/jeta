package com.github.brooth.metacode.pubsub;

/**
 *
 */
public @interface Subscribe {

    Class<?>[] value();

    Class<? extends IFilter>[] filters() default {};

    int[] ids() default {};

    String[] topics() default {};

    int priority() default 0;
}
