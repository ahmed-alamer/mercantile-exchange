package com.hydra.merc.contract;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created By ahmed on 07-13-2019
 */
@Repository
public interface ContractSpecificationsRepo extends CrudRepository<ContractSpecifications, Long> {
}
