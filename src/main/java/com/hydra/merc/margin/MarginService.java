package com.hydra.merc.margin;

import com.google.common.collect.ImmutableList;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.margin.requirements.MarginRequirement;
import com.hydra.merc.margin.requirements.MarginRequirementsRepo;
import com.hydra.merc.margin.result.MarginOpenError;
import com.hydra.merc.margin.result.MarginOpenResult;
import com.hydra.merc.margin.result.MarginResult;
import com.hydra.merc.margin.transactions.MarginLedger;
import com.hydra.merc.margin.transactions.MarginTransaction;
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

    private final Ledger ledger;
    private final MarginLedger marginLedger;

    @Autowired
    public MarginService(MarginsRepo marginsRepo,
                         MarginRequirementsRepo marginRequirementsRepo,
                         Ledger ledger,
                         MarginLedger marginLedger) {
        this.marginsRepo = marginsRepo;
        this.marginRequirementsRepo = marginRequirementsRepo;
        this.ledger = ledger;
        this.marginLedger = marginLedger;
    }


    private Optional<MarginOpenError> checkBalances(Account buyer, Account seller, float initialMargin) {
        var buyerBalance = ledger.getAccountBalance(buyer);
        var sellerBalance = ledger.getAccountBalance(seller);

        if (buyerBalance < initialMargin && sellerBalance < initialMargin) {
            return Optional.of(MarginOpenError.of(ImmutableList.of(buyer, seller),
                                                  "insufficient funds in both accounts"));
        }

        if (buyerBalance < initialMargin) {
            return Optional.of(MarginOpenError.of(ImmutableList.of(buyer), "insufficient buyer funds"));
        }

        if (sellerBalance < initialMargin) {
            return Optional.of(MarginOpenError.of(ImmutableList.of(seller), "insufficient seller funds"));
        }

        return Optional.empty();
    }

    public Optional<Margin> getMargin(Account account, Contract contract) {
        return marginsRepo.findByAccountAndContract(account, contract);
    }

    public Settlement.Leg creditMargin(Margin margin, float amount) {
        return marginLedger.credit(margin, amount);
    }

    public Settlement.Leg debitMargin(Margin margin, float amount) {
        var contract = margin.getContract();
        var currentCollateralBalance = marginLedger.getBalance(margin);

        var requiredCollateral = marginRequirementsRepo.findByContractAndPeriod(contract,
                                                                                LocalDate.now(),
                                                                                contract.getExpirationDate())
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

        return marginLedger.debit(margin, amount);
    }

    private MarginSettlementResult issueMarginCall(Margin margin, float remainingCollateral, float requiredCollateral) {
        var marginCallAmount = requiredCollateral - remainingCollateral;

        var marginCallTx = marginLedger.issueMarginCall(margin, marginCallAmount);
        var marginCallLedgerTx = ledger.debitMarginCall(margin.getAccount(), marginCallAmount);

        return MarginSettlementResult.of(marginCallTx, marginCallLedgerTx);
    }

    public MarginResult<MarginOpenResult> openMargins(Contract contract,
                                                      Account buyer,
                                                      Account seller,
                                                      float price,
                                                      int quantity) {

        float initialMargin = getInitialMargin(contract, quantity);
        var insufficientFundForMargin = checkBalances(buyer, seller, initialMargin);
        if (insufficientFundForMargin.isPresent()) {
            return MarginResult.error(MarginResult.Type.INSUFFICIENT_FUNDS, insufficientFundForMargin.get());
        }

        var buyerResult = openMargin(contract, buyer, price, quantity, initialMargin);
        var sellerResult = openMargin(contract, seller, price, quantity, initialMargin);

        return MarginResult.success(MarginResult.Type.OPEN,
                                    MarginOpenResult.of(buyerResult, sellerResult, initialMargin));
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

        return marginLedger.openMargin(margin, collateral);
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

    public MarginSettlementResult closeMargin(Margin margin) {
        var balance = marginLedger.getBalance(margin);

        MarginTransaction marginTransaction = marginLedger.closeMargin(margin);

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

        return MarginSettlementResult.of(marginTransaction, ledgerTransaction);
    }


}
