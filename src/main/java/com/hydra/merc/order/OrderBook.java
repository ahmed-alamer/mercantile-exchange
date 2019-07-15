package com.hydra.merc.order;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.position.Position;
import com.hydra.merc.position.PositionType;
import com.hydra.merc.position.PositionsService;
import com.hydra.merc.price.DailyPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created By aalamer on 07-15-2019
 */
@Service
public class OrderBook {
    private final Map<Order.Direction, Multimap<Contract, Order>> openInterest = ImmutableMap.of(
            Order.Direction.LONG, ArrayListMultimap.create(),
            Order.Direction.SHORT, ArrayListMultimap.create()
    );


    private final OrdersRepo ordersRepo;

    private final PositionsService positionsService;

    private final DailyPriceService dailyPriceService;

    @Autowired
    public OrderBook(OrdersRepo ordersRepo, PositionsService positionsService, DailyPriceService dailyPriceService) {
        this.ordersRepo = ordersRepo;
        this.positionsService = positionsService;
        this.dailyPriceService = dailyPriceService;
    }

    public Order submitOrder(Account account, Contract contract, Order.Direction direction, int quantity) {
        var order = new Order()
                .setAccount(account)
                .setContract(contract)
                .setDirection(direction)
                .setQuantity(quantity);


        var book = openInterest.get(direction);
        var anteBook = openInterest.get(direction.getAnte());

        var maybeMatch = anteBook.values().stream().filter(ante -> ante.getQuantity() == quantity).findFirst();
        if (maybeMatch.isEmpty()) {
            book.put(contract, order);
        } else {
            Order match = maybeMatch.get();

            var position = new Position()
                    .setType(PositionType.OPEN)
                    .setQuantity(quantity)
                    .setPrice(0f)
                    .setSeller(match.getAccount())
                    .setBuyer(account)
                    .setContract(contract);

            positionsService.openPosition(position); // TODO: Notification Service

            //TODO: Remove the ante order
        }


        return ordersRepo.save(order);
    }
}
