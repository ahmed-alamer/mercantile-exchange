package com.hydra.merc.utils;

/**
 * Created By ahmed on 07-13-2019
 */
public interface IdHandler<T, ID> {
    void setId(T entity, ID id);

    ID getId(T entity);

    ID defaultValue();
}
