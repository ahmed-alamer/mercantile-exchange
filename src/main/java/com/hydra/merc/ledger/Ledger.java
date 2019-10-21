package com.hydra.merc.ledger;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public LedgerTransaction deposit(Account account, float amount) {
        var depositTransaction = new LedgerTransaction()
                .setAmount(amount)
                .setCredit(account)
                .setDebit(Account.CASH_ACCOUNT);

        return ledgerTransactionsRepo.save(depositTransaction);
    }

    @Transactional
    public TradeResult debitMargin(Account buyer, Account seller, float initialMargin) {
        var buyerTransaction = new LedgerTransaction()
                .setAmount(initialMargin)
                .setCredit(Account.MARGINS_ACCOUNT)
                .setDebit(buyer);

        var sellerTransaction = new LedgerTransaction()
                .setAmount(initialMargin)
                .setCredit(Account.MARGINS_ACCOUNT)
                .setDebit(seller);


        return TradeResult.of(buyerTransaction, sellerTransaction);
    }


    public TradeResult debitFees(Contract contract, Account buyer, Account seller, float price, float fee) {
        var underlying = contract.getSpecifications().getUnderlying();
        var notional = price * underlying;
        var feeAmount = notional * fee;

        var buyerFee = new LedgerTransaction()
                .setAmount(feeAmount)
                .setCredit(Account.FEES_ACCOUNT)
                .setDebit(buyer);

        var sellerFee = new LedgerTransaction()
                .setAmount(feeAmount)
                .setCredit(Account.FEES_ACCOUNT)
                .setDebit(seller);

        return TradeResult.of(buyerFee, sellerFee);
    }

    private float totalTransactionsNotional(Account account,
                                            Function<Account, List<LedgerTransaction>> transactionsSupplier) {
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

    public LedgerTransaction debitMarginCall(Account account, float amount) {
        var marginCallLedgerTransaction = new LedgerTransaction()
                .setAmount(amount)
                .setDebit(account)
                .setCredit(Account.MARGINS_ACCOUNT);

        return ledgerTransactionsRepo.save(marginCallLedgerTransaction);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static final class TradeResult {
        private LedgerTransaction buyer;
        private LedgerTransaction seller;
    }
}
