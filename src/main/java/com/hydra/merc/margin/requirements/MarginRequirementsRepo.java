package com.hydra.merc.margin.requirements;

import com.hydra.merc.contract.Contract;
import org.joda.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface MarginRequirementsRepo extends CrudRepository<MarginRequirement, Long> {
    @Query("from MarginRequirement m where contract = :contract and (endDate < :endDate and startDate > :startDate) order by startDate desc")
    Optional<MarginRequirement> findByContractAndPeriod(@Param("contract") Contract contract, @Param("startDate") LocalDate start, @Param("endDate") LocalDate end);
}
