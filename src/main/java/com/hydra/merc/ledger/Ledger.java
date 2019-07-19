package com.hydra.merc.ledger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hydra.merc.account.Account;
import com.hydra.merc.position.Position;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created By aalamer on 07-10-2019
 */
@Service
public class Ledger {

    private final LedgerTransactionsRepo ledgerTransactionsRepo;

    @Autowired
    public Ledger(LedgerTransactionsRepo ledgerTransactionsRepo) {
        this.ledgerTransactionsRepo = ledgerTransactionsRepo;
    }

    public float getAccountBalance(Account account) {
        Float totalCredit = totalTransactionsNotional(account, ledgerTransactionsRepo::findAllByCredit);
        Float totalDebit = totalTransactionsNotional(account, ledgerTransactionsRepo::findAllByDebit);

        return totalCredit - totalDebit;
    }

    public List<LedgerTransaction> getTransactionsForPeriod(Account account, DateTime start, DateTime end) {
        return ImmutableList.<LedgerTransaction>builder()
                .addAll(ledgerTransactionsRepo.findAllByCredit(account))
                .addAll(ledgerTransactionsRepo.findAllByDebit(account))
                .build();
    }

    public LedgerTransaction deposit(Account account, float amount) {
        var depositTransaction = new LedgerTransaction()
                .setAmount(amount)
                .setCredit(account)
                .setDebit(Account.CASH_ACCOUNT);

        return ledgerTransactionsRepo.save(depositTransaction);
    }

    @Transactional
    public List<LedgerTransaction> debitMargin(Position position, float initialMargin) {
        var buyerTransaction = new LedgerTransaction()
                .setAmount(initialMargin)
                .setCredit(Account.MARGINS_ACCOUNT)
                .setDebit(position.getBuyer());

        var sellerTransaction = new LedgerTransaction()
                .setAmount(initialMargin)
                .setCredit(Account.MARGINS_ACCOUNT)
                .setDebit(position.getSeller());


        return Lists.newArrayList(ledgerTransactionsRepo.saveAll(Arrays.asList(buyerTransaction, sellerTransaction)));
    }


    @Transactional
    public List<LedgerTransaction> debitFees(Position position, float fee) {
        var underlying = position.getContract().getSpecifications().getUnderlying();
        var price = position.getPrice();

        var notional = price * underlying;

        var feeAmount = notional * fee;

        var buyerFee = new LedgerTransaction()
                .setAmount(feeAmount)
                .setCredit(Account.FEES_ACCOUNT)
                .setDebit(position.getBuyer());

        var sellerFee = new LedgerTransaction()
                .setAmount(feeAmount)
                .setCredit(Account.FEES_ACCOUNT)
                .setDebit(position.getSeller());


        return Lists.newArrayList(Arrays.asList(buyerFee, sellerFee));
    }

    private float totalTransactionsNotional(Account account, Function<Account, List<LedgerTransaction>> transactionsSupplier) {
        return transactionsTotal(transactionsSupplier.apply(account));
    }

    private float transactionsTotal(List<LedgerTransaction> transactions) {
        return transactions.stream()
                .map(LedgerTransaction::getAmount)
                .reduce(Float::sum)
                .orElse(0f);

    }

    public LedgerTransaction submitTransaction(LedgerTransaction transaction) {
        return ledgerTransactionsRepo.save(transaction);
    }
}
