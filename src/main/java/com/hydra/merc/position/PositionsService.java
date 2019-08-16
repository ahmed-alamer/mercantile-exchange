package com.hydra.merc.position;

import com.hydra.merc.fee.FeesService;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.margin.MarginRequirement;
import com.hydra.merc.margin.MarginRequirementsRepo;
import com.hydra.merc.margin.MarginService;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        var startDate = LocalDate.now();
        var endDate = startDate.plusDays(10);
        var marginRequirement = marginRequirementsRepo.findByContractAndPeriod(contract, startDate, endDate);

        var defaultInitialMargin = contract.getSpecifications().getInitialMargin();
        var contractInitialMargin = marginRequirement.map(MarginRequirement::getInitialMargin).orElse(defaultInitialMargin);

        var initialMargin = contractInitialMargin * position.getQuantity();

        if (buyerBalance < initialMargin || sellerBalance < initialMargin) {
            return new Ticket()
                    .setPosition(position)
                    .setType(TicketType.INSUFFICIENT_FUNDS);
        }


        var debits = ledger.debitMargin(position, initialMargin);

        var contractFee = feesService.getContractFee(contract);
        var fees = ledger.debitFees(position, contractFee);

        positionsRepo.save(position);

        var marginOpenResult = marginService.openMargins(position, initialMargin);

        return new Ticket()
                .setType(TicketType.FILL)
                .setPosition(position)
                .setBuyer(Ticket.Leg.of(marginOpenResult.getBuyer(), debits.getBuyer(), fees.getBuyer()))
                .setSeller(Ticket.Leg.of(marginOpenResult.getSeller(), debits.getSeller(), fees.getSeller()));
    }

    // Winding down a position at expiration
    public Ticket closePosition(Position position) {
        var closeResult = marginService.closeMargins(position);

        var buyer = closeResult.getBuyer();
        var seller = closeResult.getSeller();

        return new Ticket()
                .setType(TicketType.CONTRACT_EXPIRATION)
                .setPosition(position)
                .setBuyer(Ticket.Leg.of(buyer.getMarginTransaction(), buyer.getLedgerTransaction()))
                .setSeller(Ticket.Leg.of(seller.getMarginTransaction(), seller.getLedgerTransaction()));
    }

    public enum TicketType {
        FILL,
        INSUFFICIENT_FUNDS,
        CONTRACT_EXPIRATION
    }

}
