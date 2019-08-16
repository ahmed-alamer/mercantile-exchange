package com.hydra.merc.margin;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created By aalamer on 07-11-2019
 */
@Repository
public interface MarginsRepo extends CrudRepository<Margin, Long> {
    Optional<Margin> findByAccountAndContract(Account account, Contract contract);
}
