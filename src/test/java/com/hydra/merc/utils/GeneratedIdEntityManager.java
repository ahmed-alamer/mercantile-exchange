package com.hydra.merc.utils;

/**
 * Created By ahmed on 07-14-2019
 */
public class GeneratedIdEntityManager<T, ID> extends EntityManager<T, ID> {
    private final IdGenerator<ID> idIdGenerator;

    public GeneratedIdEntityManager(IdHandler<T, ID> idHandler, IdGenerator<ID> idIdGenerator) {
        super(idHandler);
        this.idIdGenerator = idIdGenerator;
    }

    @Override
    public T save(T entity) {
        ID id = idHandler.getId(entity);

        if (id.equals(idHandler.defaultValue())) {
            var generatedID = idIdGenerator.generate();

            idHandler.setId(entity, generatedID);
        }

        return super.save(entity);
    }
}
