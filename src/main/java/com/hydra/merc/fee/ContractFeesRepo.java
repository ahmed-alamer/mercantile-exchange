package com.hydra.merc.fee;

import com.hydra.merc.contract.Contract;
import org.joda.time.DateTime;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface ContractFeesRepo extends CrudRepository<ContractFee, Long> {

    Optional<ContractFee> findByContractAndEndBeforeOrderByStartDesc(Contract contract, DateTime end);
}
