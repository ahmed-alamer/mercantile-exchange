package com.hydra.merc.margin;

import com.google.common.collect.Lists;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.position.Position;
import com.hydra.merc.price.DailyPriceRepo;
import lombok.AllArgsConstructor;
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

        // TODO: Method!
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

            var marginCallTransaction = new LedgerTransaction()
                    .setAmount(remainingCollateral + requiredCollateral)
                    .setDebit(counterparts.longCounterpart.getAccount())
                    .setCredit(Account.MARGINS_ACCOUNT);

            ledgerTransactions.add(ledger.submitTransaction(marginCallTransaction));
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

    @AllArgsConstructor(staticName = "of")
    private static final class Counterparts {
        private final Margin longCounterpart;
        private final Margin shortCounterpart;
    }

}
