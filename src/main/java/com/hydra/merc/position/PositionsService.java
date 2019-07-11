package com.hydra.merc.position;

import com.google.common.collect.Lists;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.fee.FeesService;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.margin.MarginRequirement;
import com.hydra.merc.margin.MarginRequirementsRepo;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Service
public class PositionsService {
    private final Ledger ledger;
    private final FeesService feesService;

    private final PositionsRepo positionsRepo;
    private final MarginRequirementsRepo marginRequirementsRepo;

    @Autowired
    public PositionsService(Ledger ledger,
                            FeesService feesService,
                            PositionsRepo positionsRepo,
                            MarginRequirementsRepo marginRequirementsRepo) {
        this.positionsRepo = positionsRepo;
        this.ledger = ledger;
        this.feesService = feesService;
        this.marginRequirementsRepo = marginRequirementsRepo;
    }

    public Ticket openPosition(Contract contract, Account buyer, Account seller, int quantity) {
        var buyerBalance = ledger.getAccountBalance(buyer);
        var sellerBalance = ledger.getAccountBalance(seller);

        var marginRequirement = marginRequirementsRepo.findByContractAndStartAfterAndEndBeforeOrderByStartDesc(
                contract,
                DateTime.now(),
                DateTime.now()
        );

        var initialMargin = marginRequirement.map(MarginRequirement::getInitialMargin)
                .orElse(contract.getInitialMargin());

        var failedAccounts = new ArrayList<Account>();
        if (buyerBalance < initialMargin) {
            failedAccounts.add(buyer);
        }

        if (sellerBalance < initialMargin) {
            failedAccounts.add(seller);
        }

        if (!failedAccounts.isEmpty()) {
            return new Ticket()
                    .setType(TicketType.INSUFFICIENT_FUNDS)
                    .setFailedAccounts(failedAccounts);
        }

        var position = new Position()
                .setBuyer(buyer)
                .setSeller(seller)
                .setQuantity(quantity)
                .setType(PositionType.OPEN);

        var debits = ledger.debitMargin(position, initialMargin);

        var fee = feesService.getContractFee(contract);

        var fees = ledger.debitFees(position, fee);

        positionsRepo.save(position);

        return new Ticket()
                .setType(TicketType.FILL)
                .setPosition(position)
                .addTransactions(debits)
                .addTransactions(fees);
    }

    public enum TicketType {
        FILL,
        INSUFFICIENT_FUNDS,
        CONTRACT_EXPIRATION
    }

    @Data
    @Accessors(chain = true)
    public final static class Ticket {
        private TicketType type;
        private Position position;

        private List<LedgerTransaction> transactions = Lists.newArrayList();

        private List<Account> failedAccounts;

        private DateTime timestamp = DateTime.now();

        public Ticket addTransactions(List<LedgerTransaction> transactions) {
            this.transactions.addAll(transactions);
            return this;
        }
    }
}
