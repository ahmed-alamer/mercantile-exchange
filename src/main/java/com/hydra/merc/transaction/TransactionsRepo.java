package com.hydra.merc.transaction;

import com.hydra.merc.account.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface TransactionsRepo extends CrudRepository<Transaction, Long> {
    List<Transaction> findAllByCredit(Account credit);

    List<Transaction> findAllByDebit(Account debit);
}
