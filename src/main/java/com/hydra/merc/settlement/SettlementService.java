package com.hydra.merc.settlement;

import com.hydra.merc.account.Account;
import com.hydra.merc.margin.Counterparts;
import com.hydra.merc.margin.Margin;
import com.hydra.merc.margin.MarginService;
import com.hydra.merc.position.Position;
import com.hydra.merc.position.PositionsService;
import com.hydra.merc.price.DailyPriceService;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Created By aalamer on 08-20-2019
 */
// TODO: Add Quartz, then schedule every midnight at UTC
@Service
public class SettlementService {

    private final SettlementRepo settlementRepo;

    private final PositionsService positionsService;
    private final MarginService marginService;
    private final DailyPriceService dailyPriceService;


    @Autowired
    public SettlementService(SettlementRepo settlementRepo,
                             PositionsService positionsService,
                             MarginService marginService,
                             DailyPriceService dailyPriceService) {

        this.settlementRepo = settlementRepo;
        this.positionsService = positionsService;
        this.marginService = marginService;
        this.dailyPriceService = dailyPriceService;
    }

    public void run() {
        for (var openPosition : positionsService.getOpenPositions()) {
            var contractSpecifications = openPosition.getContract().getSpecifications();
            var settlementPeriod = contractSpecifications.getSettlementPeriod();

            var currentDate = DateTime.now();
            var positionOpenTime = openPosition.getOpenTime();

            var period = Period.fieldDifference(currentDate.toLocalDate(), positionOpenTime.toLocalDate());

            if (period.getDays() == settlementPeriod.getDays()) {
                var settlement = settle(openPosition);
                settlementRepo.save(settlement);
            }
        }
    }


    public Settlement settle(Position position) {
        var contract = position.getContract();

        var openPrice = position.getPrice();
        var settlementPrice = dailyPriceService.getPrice(contract).orElseThrow().getPrice();

        var priceDelta = (settlementPrice - openPrice) * position.getQuantity();

        //TODO: Make Better!
        var maybeCounterparts = buildCounterparts(position, priceDelta);
        if (maybeCounterparts.isEmpty()) {
            throw new IllegalArgumentException(String.format("Unable to find counterparts for position: %s", position));
        }

        var counterparts = maybeCounterparts.get();

        var settlementAmount = Math.abs(priceDelta);

        var longLeg = marginService.creditMargin(counterparts.getLongCounterpart(), settlementAmount);
        var shortLeg = marginService.debitMargin(counterparts.getShortCounterpart(), settlementAmount);

        return new Settlement()
                .setLongLeg(longLeg)
                .setShortLeg(shortLeg);

    }

    private Optional<Counterparts> buildCounterparts(Position position, float delta) {
        if (delta > 0) {
            return Optional.of(Counterparts.of(position.getBuyer(), position.getSeller()));
        }

        return Optional.of(Counterparts.of(position.getSeller(), position.getBuyer()));
    }

    private Margin getMargin(List<Margin> margins, Account buyer) {
        return margins.stream()
                      .filter(margin -> buyer.getId().equals(margin.getAccount().getId()))
                      .findFirst()
                      .orElseThrow();
    }
}
