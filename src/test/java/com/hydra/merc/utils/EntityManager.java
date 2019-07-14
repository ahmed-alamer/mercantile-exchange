package com.hydra.merc.utils;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Map;

/**
 * Created By ahmed on 07-13-2019
 */
public class EntityManager<T, ID> {

    @Getter
    protected final Map<ID, T> table = Maps.newHashMap();

    protected IdHandler<T, ID> idHandler;


    public EntityManager(IdHandler<T, ID> idHandler) {
        this.idHandler = idHandler;
    }

    public T save(T entity) {
        var id = idHandler.getId(entity);

        table.put(id, entity);

        return entity;
    }
}
