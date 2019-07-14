package com.hydra.merc.utils;

import java.util.function.Function;

/**
 * Created By ahmed on 07-14-2019
 */
public class StringIdHandler<T> implements IdHandler<T, String> {

    private final Function<T, String> getter;

    private StringIdHandler(Function<T, String> getter) {
        this.getter = getter;
    }

    public static <T> StringIdHandler<T> of(Function<T, String> getter) {
        return new StringIdHandler<T>(getter);
    }

    @Override
    public void setId(T entity, String s) {
        // Do Nothing
    }

    @Override
    public String getId(T entity) {
        return getter.apply(entity);
    }

    @Override
    public String defaultValue() {
        return "";
    }
}
