package com.hydra.merc.margin;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.position.Position;
import com.hydra.merc.price.DailyPriceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Created By aalamer on 07-11-2019
 */
@Service
public class MarginService {

    private final MarginsRepo marginsRepo;
    private final MarginRequirementsRepo marginRequirementsRepo;
    private final MarginTransactionsRepo marginTransactionsRepo;

    private final Ledger ledger;
    private final DailyPriceService dailyPriceService;

    @Autowired
    public MarginService(MarginsRepo marginsRepo,
                         DailyPriceService dailyPriceService,
                         MarginRequirementsRepo marginRequirementsRepo,
                         MarginTransactionsRepo marginTransactionsRepo,
                         Ledger ledger) {
        this.marginsRepo = marginsRepo;
        this.dailyPriceService = dailyPriceService;
        this.marginRequirementsRepo = marginRequirementsRepo;
        this.marginTransactionsRepo = marginTransactionsRepo;
        this.ledger = ledger;
    }

    public DailySettlement runDailySettlement(Position position) {
        var contract = position.getContract();

        var openPrice = position.getPrice();
        var settlementPrice = dailyPriceService.getPrice(contract).orElseThrow().getPrice();

        var delta = (settlementPrice - openPrice) * position.getQuantity();

        var counterparts = buildCounterparts(position, delta);
        //TODO: Make Better!
        if (counterparts.isEmpty()) {
            throw new IllegalArgumentException(String.format("Unable to find counterparts for position: %s", position));
        }

        return settle(contract, counterparts.get(), delta);
    }

    private Optional<Counterparts> buildCounterparts(Position position, float delta) {
        var buyerMargin = marginsRepo.findByAccountAndPosition(position.getBuyer(), position);
        var sellerMargin = marginsRepo.findByAccountAndPosition(position.getSeller(), position);

        if (buyerMargin.isEmpty() || sellerMargin.isEmpty()) {
            return Optional.empty();
        }

        if (delta > 0) {
            return Optional.of(Counterparts.of(buyerMargin.get(), sellerMargin.get()));
        }

        return Optional.of(Counterparts.of(sellerMargin.get(), buyerMargin.get()));
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
        var amount = Math.abs(delta);

        var longLeg = creditMargin(counterparts.longCounterpart, amount);
        var shortLeg = debitMargin(counterparts.shortCounterpart, contract, amount);


        return DailySettlement.of(longLeg, shortLeg);
    }

    private DailySettlement.Leg creditMargin(Margin margin, float amount) {
        var transaction = new MarginTransaction()
                .setCredit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        marginTransactionsRepo.save(transaction);

        return DailySettlement.Leg.create()
                .setMargin(margin)
                .setMarginTransaction(transaction);
    }

    private DailySettlement.Leg debitMargin(Margin margin,
                                            Contract contract,
                                            float amount) {

        var currentCollateralBalance = getBalance(margin);

        var requiredCollateral = marginRequirementsRepo.findByContractAndPeriod(contract, LocalDate.now(), contract.getExpirationDate())
                .map(MarginRequirement::getInitialMargin)
                .orElse(contract.getSpecifications().getInitialMargin());

        var remainingCollateral = currentCollateralBalance - amount;
        if (remainingCollateral < requiredCollateral) {
            var marginCall = processMarginCall(margin, remainingCollateral, requiredCollateral);

            return DailySettlement.Leg.create()
                    .setMargin(margin)
                    .setMarginTransaction(marginCall.getMarginTransaction())
                    .setMarginCall(marginCall.getLedgerTransaction());
        }

        var transaction = new MarginTransaction()
                .setDebit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        marginTransactionsRepo.save(transaction);

        return DailySettlement.Leg.create()
                .setMargin(margin)
                .setMarginTransaction(transaction);
    }

    private MarginSettlementResult processMarginCall(Margin margin,
                                                     float remainingCollateral,
                                                     float requiredCollateral) {
        var marginCallAmount = requiredCollateral - remainingCollateral;

        var marginCallLedgerTransaction = new LedgerTransaction()
                .setAmount(marginCallAmount)
                .setDebit(margin.getAccount())
                .setCredit(Account.MARGINS_ACCOUNT);

        var marginCallTx = new MarginTransaction()
                .setDebit(marginCallAmount)
                .setMargin(margin)
                .setType(MarginTransactionType.MARGIN_CALL);


        var marginCallLedgerTx = ledger.submitTransaction(marginCallLedgerTransaction);

        return MarginSettlementResult.of(marginCallTx, marginCallLedgerTx);
    }

    public MarginOpenResult openMargins(Position position, Float initialMargin) {
        var buyerResult = openMargin(position, position.getBuyer(), initialMargin);
        var sellerResult = openMargin(position, position.getSeller(), initialMargin);

        return MarginOpenResult.of(buyerResult, sellerResult);
    }

    public MarginResult closeMargins(Position position) {
        var margins = marginsRepo.findAllByPosition(position);
        var buyerResult = closeMargin(position.getBuyer(), getMargin(margins, position.getBuyer()));
        var sellerResult = closeMargin(position.getSeller(), getMargin(margins, position.getSeller()));

        return MarginResult.of(buyerResult, sellerResult);
    }

    public MarginSettlementResult closeMargin(Account account, Margin margin) {
        var balance = getBalance(margin);
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

        var ledgerTransaction = new LedgerTransaction()
                .setCredit(creditAccount)
                .setDebit(debitAccount)
                .setAmount(balance);

        return MarginSettlementResult.of(transaction, ledgerTransaction);
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

    public Optional<Margin> getMargin(Account account, Position position) {
        return marginsRepo.findByAccountAndPosition(account, position);
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class MarginSettlementResult {
        private final MarginTransaction marginTransaction;
        private final LedgerTransaction ledgerTransaction;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class MarginResult {
        private final MarginSettlementResult buyer;
        private final MarginSettlementResult seller;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class MarginOpenResult {
        private final MarginTransaction buyer;
        private final MarginTransaction seller;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    private static final class Counterparts {
        private final Margin longCounterpart;
        private final Margin shortCounterpart;
    }

}
