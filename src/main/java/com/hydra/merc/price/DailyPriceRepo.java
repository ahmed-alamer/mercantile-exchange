package com.hydra.merc.price;

import com.hydra.merc.contract.Contract;
import org.joda.time.LocalDate;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Created By aalamer on 07-11-2019
 */
public interface DailyPriceRepo extends CrudRepository<DailyPrice, Long> {

    Optional<DailyPrice> findDistinctByContractAndDay(Contract contract, LocalDate day);
}
