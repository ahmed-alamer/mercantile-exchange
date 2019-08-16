package com.hydra.merc.order;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.position.PositionsService;
import com.hydra.merc.price.DailyPrice;
import com.hydra.merc.price.DailyPriceService;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created By aalamer on 07-15-2019
 */
@Service
public class OrderBook {
    private final Map<Direction, Multimap<Contract, Order>> openInterest = ImmutableMap.of(
            Direction.LONG, ArrayListMultimap.create(),
            Direction.SHORT, ArrayListMultimap.create()
    );


    private final OrdersRepo ordersRepo;

    private final PositionsService positionsService;

    private final DailyPriceService dailyPriceService;

    private Map<Contract, DailyPrice> currentPrices = Maps.newHashMap();

    @Autowired
    public OrderBook(OrdersRepo ordersRepo, PositionsService positionsService, DailyPriceService dailyPriceService) {
        this.ordersRepo = ordersRepo;
        this.positionsService = positionsService;
        this.dailyPriceService = dailyPriceService;
    }

    public Order submitOrder(Account account, Contract contract, Direction direction, int quantity) {
        var order = new Order()
                .setAccount(account)
                .setContract(contract)
                .setDirection(direction)
                .setQuantity(quantity);


        var book = openInterest.get(direction);
        var anteBook = openInterest.get(direction.getAnte());

        var maybeMatch = anteBook.values().stream().filter(ante -> ante.getQuantity() == quantity).findFirst();
        if (maybeMatch.isEmpty()) {
            ordersRepo.save(order);
            book.put(contract, order);
        } else {
            var match = maybeMatch.get();

            positionsService.openPosition(contract, account, match.getAccount(), getPrice(contract), quantity); // TODO: Notification Service

            anteBook.values().removeIf(anteOrder -> anteOrder.getId() == match.getId());

            order.setStatus(OrderStatus.FILLED);
            match.setStatus(OrderStatus.FILLED);

            ordersRepo.save(match);
            ordersRepo.save(order);
        }

        return order;
    }

    public Order cancelOrder(Order order) {
        var direction = order.getDirection();
        var contract = order.getContract();

        var removed = openInterest.get(direction).get(contract).removeIf(existingOrder -> existingOrder.getId() == order.getId());
        if (removed) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        return ordersRepo.save(order);
    }

    private float getPrice(Contract contract) {
        if (currentPrices.get(contract).getDay().isBefore(LocalDate.now())) {
            currentPrices.put(contract, dailyPriceService.getPrice(contract).orElseThrow());
        }

        return currentPrices.computeIfAbsent(contract, currentContract -> dailyPriceService.getPrice(currentContract).orElseThrow()).getPrice();
    }
}
