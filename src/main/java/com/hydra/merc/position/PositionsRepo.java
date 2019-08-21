package com.hydra.merc.position;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface PositionsRepo extends CrudRepository<Position, Long> {

    public List<Position> getAllByOpenEquals(boolean open);
}
