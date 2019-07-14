package com.hydra.merc.utils;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created By ahmed on 07-14-2019
 */
public class LongIdHandler<T> implements IdHandler<T, Long> {

    private final BiConsumer<T, Long> setter;
    private final Function<T, Long> getter;

    private LongIdHandler(BiConsumer<T, Long> setter, Function<T, Long> getter) {
        this.setter = setter;
        this.getter = getter;
    }

    public static <T> LongIdHandler<T> of(BiConsumer<T, Long> setter, Function<T, Long> getter) {
        return new LongIdHandler<>(setter, getter);
    }

    @Override
    public void setId(T entity, Long id) {
        setter.accept(entity, id);
    }

    @Override
    public Long getId(T entity) {
        return getter.apply(entity);
    }

    @Override
    public Long defaultValue() {
        return 0L;
    }
}
