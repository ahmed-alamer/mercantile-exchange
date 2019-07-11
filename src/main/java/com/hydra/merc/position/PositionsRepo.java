package com.hydra.merc.position;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface PositionsRepo extends CrudRepository<Position, Long> {
}
