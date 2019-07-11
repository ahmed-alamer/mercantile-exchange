package com.hydra.merc.ledger;

import com.hydra.merc.account.Account;
import com.hydra.merc.position.Position;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface TransactionsRepo extends CrudRepository<LedgerTransaction, Long> {
    List<LedgerTransaction> findAllByCredit(Account credit);

    List<LedgerTransaction> findAllByDebit(Account debit);

    List<LedgerTransaction> findAllByPositionAndDebitAndCredit(Position position, Account debit, Account credit);
}
