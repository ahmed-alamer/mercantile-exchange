package com.hydra.merc.contract;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface ContractsRepo extends CrudRepository<Contract, Long> {
}
