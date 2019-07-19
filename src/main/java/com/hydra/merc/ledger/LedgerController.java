package com.hydra.merc.ledger;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * Created By aalamer on 07-18-2019
 */
@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final AccountService accountService;
    private final Ledger ledger;

    @Autowired
    public LedgerController(AccountService accountService, Ledger ledger) {
        this.accountService = accountService;
        this.ledger = ledger;
    }


    @GetMapping
    @RequestMapping("/{accountId}latestTransactions")
    public ResponseEntity getLatestTransactions(@PathVariable("accountId") String accountId) {
        var byId = accountService.findById(accountId);
        if (byId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var account = byId.get();

        var currentTime = DateTime.now();

        var transactions = ledger.getTransactionsForPeriod(account, currentTime.minusDays(30), currentTime)
                .stream()
                .map(transaction -> LedgerEntry.fromLedgerTransaction(account, transaction))
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactions);
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    public static final class LedgerEntry {
        private long id;
        private long accountId;
        private float debit;
        private float credit;
        private DateTime timestamp;

        public static LedgerEntry fromLedgerTransaction(Account account, LedgerTransaction transaction) {
            var entry = new LedgerEntry()
                    .setId(transaction.getId())
                    .setTimestamp(transaction.getTimestamp());

            if (transaction.getDebit().getId().equals(account.getId())) {
                entry.setDebit(transaction.getAmount());
            } else if (transaction.getCredit().getId().equals(account.getId())) {
                entry.setCredit(transaction.getAmount());
            } else {
                throw new IllegalArgumentException(String.format("Target account %s is not involved in the transaction %s", account, transaction));
            }


            return entry;
        }
    }
}
