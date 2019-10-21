package com.hydra.merc.margin.transactions;

import com.hydra.merc.margin.Margin;
import com.hydra.merc.margin.requirements.MarginRequirementsRepo;
import com.hydra.merc.settlement.Settlement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created By aalamer on 10-21-2019
 */
@Service
public class MarginLedger {
    private final MarginTransactionsRepo marginTransactionsRepo;
    private final MarginRequirementsRepo marginRequirementsRepo;

    @Autowired
    public MarginLedger(MarginTransactionsRepo marginTransactionsRepo,
                        MarginRequirementsRepo marginRequirementsRepo) {
        this.marginTransactionsRepo = marginTransactionsRepo;
        this.marginRequirementsRepo = marginRequirementsRepo;
    }

    public float getBalance(Margin margin) {
        return marginTransactionsRepo.findAllByMargin(margin)
                                     .stream()
                                     .map(transaction -> transaction.getCredit() - transaction.getDebit())
                                     .reduce(Float::sum)
                                     .orElse(0f);
    }

    public Settlement.Leg credit(Margin margin, float amount) {
        var transaction = new MarginTransaction()
                .setCredit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        marginTransactionsRepo.save(transaction);

        return Settlement.Leg.create()
                             .setMargin(margin)
                             .setMarginTransaction(transaction);

    }

    public Settlement.Leg debit(Margin margin, float amount) {
        var transaction = new MarginTransaction()
                .setDebit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        marginTransactionsRepo.save(transaction);

        return Settlement.Leg.create()
                             .setMargin(margin)
                             .setMarginTransaction(transaction);

    }

    public MarginTransaction issueMarginCall(Margin margin, float marginCallAmount) {
        var marginTransaction = new MarginTransaction()
                .setDebit(marginCallAmount)
                .setMargin(margin)
                .setType(MarginTransactionType.MARGIN_CALL);

        return marginTransactionsRepo.save(marginTransaction);
    }

    public MarginTransaction openMargin(Margin margin, float collateral) {
        var initialMarginTransaction = new MarginTransaction()
                .setType(MarginTransactionType.OPEN)
                .setCredit(collateral)
                .setMargin(margin);

        return marginTransactionsRepo.save(initialMarginTransaction);
    }

    public MarginTransaction closeMargin(Margin margin) {
        var balance = getBalance(margin);

        var transaction = new MarginTransaction()
                .setMargin(margin)
                .setType(MarginTransactionType.CLOSE);

        if (balance > 0) {
            transaction.setDebit(balance);
        } else {
            transaction.setCredit(Math.abs(balance));
        }

        return marginTransactionsRepo.save(transaction);
    }
}
