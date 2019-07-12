package com.hydra.merc.position;

import com.hydra.merc.account.Account;
import com.hydra.merc.fee.FeesService;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.margin.MarginRequirement;
import com.hydra.merc.margin.MarginRequirementsRepo;
import com.hydra.merc.margin.MarginService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Created By aalamer on 07-10-2019
 */
@Service
public class PositionsService {
    private final Ledger ledger;
    private final FeesService feesService;

    private final PositionsRepo positionsRepo;
    private final MarginRequirementsRepo marginRequirementsRepo;

    private final MarginService marginService;

    @Autowired
    public PositionsService(Ledger ledger,
                            FeesService feesService,
                            PositionsRepo positionsRepo,
                            MarginRequirementsRepo marginRequirementsRepo,
                            MarginService marginService) {
        this.positionsRepo = positionsRepo;
        this.ledger = ledger;
        this.feesService = feesService;
        this.marginRequirementsRepo = marginRequirementsRepo;
        this.marginService = marginService;
    }

    @Transactional
    public Ticket openPosition(Position position) {
        var contract = position.getContract();
        var buyer = position.getBuyer();
        var seller = position.getSeller();

        var buyerBalance = ledger.getAccountBalance(buyer);
        var sellerBalance = ledger.getAccountBalance(seller);

        var marginRequirement = marginRequirementsRepo.findByContractAndStartAfterAndEndBeforeOrderByStartDesc(
                contract,
                DateTime.now(),
                DateTime.now()
        );

        var initialMargin = marginRequirement.map(MarginRequirement::getInitialMargin).orElse(contract.getInitialMargin());

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


        var debits = ledger.debitMargin(position, initialMargin);

        var fee = feesService.getContractFee(contract);

        var fees = ledger.debitFees(position, fee);


        positionsRepo.save(position);

        var marginTransactions = marginService.openMargins(position, initialMargin);

        return new Ticket()
                .setType(TicketType.FILL)
                .setPosition(position)
                .addTransactions(debits)
                .addTransactions(fees)
                .addMarinTransactions(marginTransactions);
    }

    // Winding down a position at expiration
    public Ticket closePosition(Position position) {
        var closeResult = marginService.closeMargins(position);

        return new Ticket()
                .setType(TicketType.CONTRACT_EXPIRATION)
                .setPosition(position)
                .addTransactions(closeResult.stream().map(MarginService.MarginCloseResult::getLedgerTransaction).collect(Collectors.toList()))
                .addMarinTransactions(closeResult.stream().map(MarginService.MarginCloseResult::getMarginTransaction).collect(Collectors.toList()));
    }

    public enum TicketType {
        FILL,
        INSUFFICIENT_FUNDS,
        CONTRACT_EXPIRATION
    }

}
