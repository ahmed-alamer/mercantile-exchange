package com.hydra.merc.margin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.position.Position;
import com.hydra.merc.price.DailyPriceRepo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created By aalamer on 07-11-2019
 */
@Service
public class MarginService {

    private final MarginsRepo marginsRepo;
    private final DailyPriceRepo dailyPriceRepo;
    private final MarginRequirementsRepo marginRequirementsRepo;
    private final MarginTransactionsRepo marginTransactionsRepo;

    private final Ledger ledger;

    @Autowired
    public MarginService(MarginsRepo marginsRepo,
                         DailyPriceRepo dailyPriceRepo,
                         MarginRequirementsRepo marginRequirementsRepo,
                         MarginTransactionsRepo marginTransactionsRepo,
                         Ledger ledger) {
        this.marginsRepo = marginsRepo;
        this.dailyPriceRepo = dailyPriceRepo;
        this.marginRequirementsRepo = marginRequirementsRepo;
        this.marginTransactionsRepo = marginTransactionsRepo;
        this.ledger = ledger;
    }

    public DailySettlement runDailySettlement(Position position) {
        var contract = position.getContract();

        var margins = marginsRepo.findAllByPosition(position);

        var buyerMargin = getMargin(margins, position.getBuyer());
        var sellerMargin = getMargin(margins, position.getSeller());

        var openPrice = position.getPrice();
        var settlementPrice = dailyPriceRepo.findContractAndDay(contract, LocalDate.now()).orElseThrow().getPrice();

        var delta = settlementPrice - openPrice;


        var counterparts = delta > 0
                ? Counterparts.of(buyerMargin, sellerMargin)
                : Counterparts.of(sellerMargin, buyerMargin);

        return settle(contract, counterparts, delta);
    }

    private Margin getMargin(List<Margin> margins, Account buyer) {
        return margins.stream().filter(margin -> buyer.getId().equals(margin.getAccount().getId())).findFirst().orElseThrow();
    }

    public float getBalance(Margin margin) {
        return marginTransactionsRepo.findAllByMargin(margin)
                .stream()
                .map(transaction -> transaction.getCredit() - transaction.getDebit())
                .reduce(Float::sum)
                .orElse(0f);
    }

    private DailySettlement settle(Contract contract, Counterparts counterparts, float delta) {
        var ledgerTransactions = Lists.<LedgerTransaction>newArrayList();
        var marginTransactions = Lists.<MarginTransaction>newArrayList();

        marginTransactions.add(creditMargin(counterparts.longCounterpart, delta));

        float collateral = getBalance(counterparts.shortCounterpart);

        var remainingCollateral = collateral - delta;
        if (remainingCollateral <= 0) {
            var requiredCollateral = marginRequirementsRepo.findByContractAndStartAfterAndEndBeforeOrderByStartDesc(contract, DateTime.now(), contract.getExpirationDate().toDateTimeAtStartOfDay())
                    .map(MarginRequirement::getInitialMargin)
                    .orElse(contract.getInitialMargin());

            float marginCallAmount = remainingCollateral + requiredCollateral;

            var marginCallLedgerTransaction = new LedgerTransaction()
                    .setAmount(marginCallAmount)
                    .setDebit(counterparts.longCounterpart.getAccount())
                    .setCredit(Account.MARGINS_ACCOUNT);

            var marginCall = new MarginTransaction()
                    .setDebit(marginCallAmount)
                    .setMargin(counterparts.longCounterpart)
                    .setType(MarginTransactionType.MARGIN_CALL);

            marginTransactions.add(marginCall);
            ledgerTransactions.add(ledger.submitTransaction(marginCallLedgerTransaction));
        } else {
            marginTransactions.add(debitMargin(counterparts.shortCounterpart, delta));
        }

        return DailySettlement.of(counterparts.longCounterpart, counterparts.shortCounterpart, ledgerTransactions, marginTransactions);
    }

    private MarginTransaction creditMargin(Margin margin, float amount) {
        var transaction = new MarginTransaction()
                .setCredit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        return marginTransactionsRepo.save(transaction);
    }

    private MarginTransaction debitMargin(Margin margin, float amount) {
        var transaction = new MarginTransaction()
                .setDebit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        return marginTransactionsRepo.save(transaction);
    }

    public List<MarginTransaction> openMargins(Position position, Float initialMargin) {
        Account buyer = position.getBuyer();
        Account seller = position.getSeller();

        return Lists.newArrayList(openMargin(position, buyer, initialMargin), openMargin(position, seller, initialMargin));
    }

    public List<MarginCloseResult> closeMargins(Position position) {
        var margins = marginsRepo.findAllByPosition(position);

        return ImmutableList.<MarginCloseResult>builder()
                .add(closeMargin(position.getBuyer(), getMargin(margins, position.getBuyer())))
                .add(closeMargin(position.getSeller(), getMargin(margins, position.getSeller())))
                .build();
    }

    public MarginCloseResult closeMargin(Account account, Margin margin) {
        float balance = getBalance(margin);
        var transaction = new MarginTransaction()
                .setMargin(margin)
                .setType(MarginTransactionType.CLOSE);

        if (balance > 0) {
            transaction.setCredit(balance);
        } else {
            transaction.setDebit(balance);
        }

        var creditAccount = balance > 0
                ? account
                : Account.MARGINS_ACCOUNT;

        var debitAccount = balance > 0
                ? Account.MARGINS_ACCOUNT
                : account;

        LedgerTransaction ledgerTransaction = new LedgerTransaction()
                .setCredit(creditAccount)
                .setDebit(debitAccount)
                .setAmount(balance);

        return MarginCloseResult.of(transaction, ledgerTransaction);
    }

    private MarginTransaction openMargin(Position position, Account account, Float initialMargin) {
        var margin = new Margin()
                .setAccount(account)
                .setPosition(position)
                .setCollateral(initialMargin);

        marginsRepo.save(margin);

        var initialMarginTransaction = new MarginTransaction()
                .setType(MarginTransactionType.OPEN)
                .setCredit(initialMargin)
                .setMargin(margin);

        return marginTransactionsRepo.save(initialMarginTransaction);
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class MarginCloseResult {
        private final MarginTransaction marginTransaction;
        private final LedgerTransaction ledgerTransaction;
    }

    @AllArgsConstructor(staticName = "of")
    private static final class Counterparts {
        private final Margin longCounterpart;
        private final Margin shortCounterpart;
    }

}
