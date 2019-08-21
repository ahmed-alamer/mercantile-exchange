package com.hydra.merc.position;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.fee.FeesService;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.margin.MarginResult;
import com.hydra.merc.margin.MarginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Service
public class PositionsService {
    private final Ledger ledger;
    private final FeesService feesService;

    private final PositionsRepo positionsRepo;

    private final MarginService marginService;

    @Autowired
    public PositionsService(Ledger ledger,
                            FeesService feesService,
                            PositionsRepo positionsRepo,
                            MarginService marginService) {
        this.positionsRepo = positionsRepo;
        this.ledger = ledger;
        this.feesService = feesService;
        this.marginService = marginService;
    }

    @Transactional
    public Ticket openPosition(Contract contract, Account buyer, Account seller, float price, int quantity) {
        var marginResult = marginService.openMargins(contract, buyer, seller, price, quantity);

        switch (marginResult.getType()) {
            case OPEN:
                return handleOpen(contract, buyer, seller, price, quantity, (MarginResult.MarginOpenResult) marginResult);
            case INSUFFICIENT_FUNDS:
                return handleInsufficientFunds();
            default:
                throw new IllegalStateException(String.format("Unexpected return type: %s", marginResult.getType()));
        }
    }

    public Ticket closePosition(Position position) {
        var buyer = marginService.closeMargin(position.getBuyer());
        var seller = marginService.closeMargin(position.getSeller());

        return new Ticket()
                .setType(TicketType.CONTRACT_EXPIRATION)
                .setPosition(position)
                .setBuyer(Ticket.Leg.of(buyer.getMarginTransaction(), buyer.getLedgerTransaction()))
                .setSeller(Ticket.Leg.of(seller.getMarginTransaction(), seller.getLedgerTransaction()));
    }

    private Ticket handleInsufficientFunds() {
        return new Ticket().setType(TicketType.INSUFFICIENT_FUNDS);
    }

    private Ticket handleOpen(Contract contract,
                              Account buyer,
                              Account seller,
                              float price,
                              int quantity,
                              MarginResult.MarginOpenResult marginOpenResult) {

        var initialMargin = marginOpenResult.getInitialMargin();

        var debits = ledger.debitMargin(buyer, seller, initialMargin);

        var contractFee = feesService.getContractFee(contract);
        var fees = ledger.debitFees(contract, buyer, seller, price, contractFee);

        var position = new Position()
                .setBuyer(marginOpenResult.getBuyer().getMargin())
                .setSeller(marginOpenResult.getSeller().getMargin())
                .setContract(contract)
                .setPrice(price)
                .setQuantity(quantity)
                .setCollateral(initialMargin)
                .setType(PositionType.OPEN);

        positionsRepo.save(position);

        return new Ticket()
                .setType(TicketType.FILL)
                .setPosition(position)
                .setBuyer(Ticket.Leg.of(marginOpenResult.getBuyer(), debits.getBuyer(), fees.getBuyer()))
                .setSeller(Ticket.Leg.of(marginOpenResult.getSeller(), debits.getSeller(), fees.getSeller()));
    }
    // Winding down a position at expiration

    public List<Position> getOpenPositions() {
        return positionsRepo.getAllByOpenEquals(true);
    }

    public enum TicketType {
        FILL,
        INSUFFICIENT_FUNDS,
        CONTRACT_EXPIRATION
    }

}
