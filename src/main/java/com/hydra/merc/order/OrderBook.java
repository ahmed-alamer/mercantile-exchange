package com.hydra.merc.order;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.position.PositionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Created By aalamer on 07-15-2019
 */
@Slf4j
@Service
public class OrderBook {
    private final Map<Direction, Multimap<Long, OpenInterest>> openInterest = ImmutableMap.of(
            Direction.LONG, ArrayListMultimap.create(),
            Direction.SHORT, ArrayListMultimap.create()
    );

    private final OrdersRepo ordersRepo;

    private final PositionsService positionsService;

    @Autowired
    public OrderBook(OrdersRepo ordersRepo, PositionsService positionsService) {
        this.ordersRepo = ordersRepo;
        this.positionsService = positionsService;
    }

    @Transactional
    public Order submitOrder(Order order) {
        var account = order.getAccount();
        var contract = order.getContract();
        var direction = order.getDirection();

        var price = order.getPrice();
        var quantity = order.getQuantity();

        var book = openInterest.get(direction);
        var anteBook = openInterest.get(direction.getAnte());

        var maybeMatch = anteBook.values()
                                 .stream()
                                 .filter(ante -> ante.getOrder().getQuantity() == quantity)
                                 .findFirst();
        if (maybeMatch.isEmpty()) {
            log.debug("Adding new order to {}", direction);
            ordersRepo.save(order.setStatus(OrderStatus.OPEN));
            book.put(contract.getId(), OpenInterest.of(contract, order));
        } else {
            log.debug("Matching new order on {}/{}", direction, direction.getAnte());
            var match = maybeMatch.get().getOrder();

            log.debug("Order: {}", order);
            log.debug("Match: {}", match);

            positionsService.openPosition(contract, account, match.getAccount(), price, quantity); // TODO: Notification Service

            anteBook.values().removeIf(anteOrder -> anteOrder.getOrder().getId() == match.getId());

            match.setStatus(OrderStatus.FILLED);
            order.setStatus(OrderStatus.FILLED);

            ordersRepo.save(match);
            ordersRepo.save(order);
        }

        return order;
    }

    public Order cancelOrder(Order order) {
        var direction = order.getDirection();
        var contract = order.getContract();

        var removed = openInterest.get(direction)
                                  .get(contract.getId())
                                  .removeIf(openInterest -> openInterest.getOrder().getId() == order.getId());
        if (removed) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        return ordersRepo.save(order);
    }

    public List<OpenInterest> getOpenInterestForContract(Contract contract, Direction direction) {
        return Lists.newArrayList(openInterest.get(direction).get(contract.getId()));
    }
}
