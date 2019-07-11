package com.hydra.merc.margin;

import com.hydra.merc.contract.Contract;
import org.joda.time.DateTime;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface MarginRequirementsRepo extends CrudRepository<MarginRequirement, Long> {
    Optional<MarginRequirement> findByContractAndStartAfterAndEndBeforeOrderByStartDesc(Contract contract, DateTime start, DateTime end);
}
