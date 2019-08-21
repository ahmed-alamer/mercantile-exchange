package com.hydra.merc.margin;

import com.google.common.collect.ImmutableList;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.settlement.Settlement;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    public MarginService(MarginsRepo marginsRepo,
                         MarginRequirementsRepo marginRequirementsRepo,
                         MarginTransactionsRepo marginTransactionsRepo,
                         Ledger ledger) {
        this.marginsRepo = marginsRepo;
        this.marginRequirementsRepo = marginRequirementsRepo;
        this.marginTransactionsRepo = marginTransactionsRepo;
        this.ledger = ledger;
    }


    private MarginSettlementResult issueMarginCall(Margin margin,
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

    public MarginResult openMargins(Contract contract, Account buyer, Account seller, float price, int quantity) {
        float initialMargin = getInitialMargin(contract, quantity);
        var insufficientFundForMargin = checkBalances(buyer, seller, initialMargin);
        if (insufficientFundForMargin.isPresent()) {
            return insufficientFundForMargin.get();
        }

        var buyerResult = openMargin(contract, buyer, price, quantity, initialMargin);
        var sellerResult = openMargin(contract, seller, price, quantity, initialMargin);

        return MarginResult.MarginOpenResult.of(buyerResult, sellerResult, initialMargin);
    }

    private Optional<MarginResult.InsufficientFundForMargin> checkBalances(Account buyer,
                                                                           Account seller,
                                                                           float initialMargin) {
        var buyerBalance = ledger.getAccountBalance(buyer);
        var sellerBalance = ledger.getAccountBalance(seller);

        if (buyerBalance < initialMargin && sellerBalance < initialMargin) {
            return Optional.of(MarginResult.InsufficientFundForMargin.of(ImmutableList.of(buyer, seller)));
        }

        if (buyerBalance < initialMargin) {
            return Optional.of(MarginResult.InsufficientFundForMargin.of(ImmutableList.of(buyer)));
        }

        if (sellerBalance < initialMargin) {
            return Optional.of(MarginResult.InsufficientFundForMargin.of(ImmutableList.of(seller)));
        }

        return Optional.empty();
    }

    public MarginSettlementResult closeMargin(Margin margin) {
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
                ? margin.getAccount()
                : Account.MARGINS_ACCOUNT;

        var debitAccount = balance > 0
                ? Account.MARGINS_ACCOUNT
                : margin.getAccount();

        var ledgerTransaction = new LedgerTransaction()
                .setCredit(creditAccount)
                .setDebit(debitAccount)
                .setAmount(balance);

        return MarginSettlementResult.of(transaction, ledgerTransaction);
    }

    public Optional<Margin> getMargin(Account account, Contract contract) {
        return marginsRepo.findByAccountAndContract(account, contract);
    }

    public float getBalance(Margin margin) {
        return marginTransactionsRepo.findAllByMargin(margin)
                .stream()
                .map(transaction -> transaction.getCredit() - transaction.getDebit())
                .reduce(Float::sum)
                .orElse(0f);
    }

    public Settlement.Leg creditMargin(Margin margin, float amount) {
        var transaction = new MarginTransaction()
                .setCredit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        marginTransactionsRepo.save(transaction);

        return Settlement.Leg.create()
                .setMargin(margin)
                .setMarginTransaction(transaction);
    }

    public Settlement.Leg debitMargin(Margin margin, Contract contract, float amount) {
        var currentCollateralBalance = getBalance(margin);

        var requiredCollateral = marginRequirementsRepo.findByContractAndPeriod(contract, LocalDate.now(), contract.getExpirationDate())
                .map(MarginRequirement::getInitialMargin)
                .orElse(contract.getSpecifications().getInitialMargin());

        var remainingCollateral = currentCollateralBalance - amount;
        if (remainingCollateral < requiredCollateral) {
            var marginCall = issueMarginCall(margin, remainingCollateral, requiredCollateral);

            return Settlement.Leg.create()
                    .setMargin(margin)
                    .setMarginTransaction(marginCall.getMarginTransaction())
                    .setMarginCall(marginCall.getLedgerTransaction());
        }

        var transaction = new MarginTransaction()
                .setDebit(amount)
                .setMargin(margin)
                .setType(MarginTransactionType.SETTLEMENT);

        marginTransactionsRepo.save(transaction);

        return Settlement.Leg.create()
                .setMargin(margin)
                .setMarginTransaction(transaction);
    }


    private MarginTransaction openMargin(Contract contract,
                                         Account account,
                                         float price,
                                         int quantity,
                                         float collateral) {
        var margin = new Margin()
                .setContract(contract)
                .setPrice(price)
                .setQuantity(quantity)
                .setAccount(account)
                .setCollateral(collateral);

        marginsRepo.save(margin);

        var initialMarginTransaction = new MarginTransaction()
                .setType(MarginTransactionType.OPEN)
                .setCredit(collateral)
                .setMargin(margin);

        return marginTransactionsRepo.save(initialMarginTransaction);
    }

    private float getInitialMargin(Contract contract, int quantity) {
        var startDate = LocalDate.now();
        var endDate = startDate.plusDays(10);
        var marginRequirement = marginRequirementsRepo.findByContractAndPeriod(contract, startDate, endDate);

        var defaultInitialMargin = contract.getSpecifications().getInitialMargin();
        var contractInitialMargin = marginRequirement.map(MarginRequirement::getInitialMargin)
                                                     .orElse(defaultInitialMargin);

        return contractInitialMargin * quantity;
    }


}
