package com.hydra.merc.ledger;

import com.google.common.collect.Lists;
import com.hydra.merc.account.Account;
import com.hydra.merc.position.Position;
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

    private final TransactionsRepo transactionsRepo;

    @Autowired
    public Ledger(TransactionsRepo transactionsRepo) {
        this.transactionsRepo = transactionsRepo;
    }

    public float getAccountBalance(Account account) {
        Float totalCredit = totalTransactionsNotional(account, transactionsRepo::findAllByCredit);
        Float totalDebit = totalTransactionsNotional(account, transactionsRepo::findAllByDebit);

        return totalCredit - totalDebit;
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


        return Lists.newArrayList(transactionsRepo.saveAll(Arrays.asList(buyerTransaction, sellerTransaction)));
    }


    public List<LedgerTransaction> debitFees(Position position, float fee) {
        var underlying = position.getContract().getUnderlying();
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
        return transactionsRepo.save(transaction);
    }
}
