package com.hydra.merc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.account.AccountsRepo;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.order.Direction;
import com.hydra.merc.order.Order;
import com.hydra.merc.order.OrderBook;
import com.hydra.merc.order.OrderStatus;
import com.hydra.merc.position.PositionsRepo;
import com.hydra.merc.position.PositionsService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Created By aalamer on 09-12-2019
 */

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
        "logging.level.com.hydra=DEBUG",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.jadira.usertype.autoRegisterUserTypes=true"
})
public class OrderBookTest {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JodaModule());

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private PositionsRepo positionsRepo;

    @Autowired
    private AccountService accountService;

    @Autowired
    private Ledger ledger;

    @Autowired
    private ContractService contractService;

    @Autowired
    private PositionsService positionsService;

    @Autowired
    private OrderBook orderBook;

    private Account seller;
    private Account buyer;

    private Contract contract;

    @Before
    public void initialize() {
        accountsRepo.saveAll(Account.INTERNAL_ACCOUNTS);

        var contractSpecs = new ContractSpecifications()
                .setInitialMargin(100)
                .setSymbol("SLR")
                .setTickSize(0.1F)
                .setUnderlying(1);

        contract = new Contract()
                .setFee(0.001F)
                .setExpirationDate(LocalDate.now().plusDays(30))
                .setIssueDate(LocalDate.now())
                .setSpecifications(contractSpecs);

        contractService.createContract(contractSpecs);
        contractService.listContract(contract);

        seller = accountService.openTradingAccount();
        buyer = accountService.openTradingAccount();

        ledger.deposit(seller, 1000);
        ledger.deposit(buyer, 1000);
    }

    @Test
    public void testMatch() {
        var sellOrder = new Order()
                .setAccount(seller)
                .setDirection(Direction.SHORT)
                .setContract(contract)
                .setPrice(10)
                .setQuantity(2)
                .setExpirationTime(new DateTime("2019-09-12T12", DateTimeZone.UTC).plusDays(1));

        var buyOrder = new Order()
                .setAccount(buyer)
                .setDirection(Direction.LONG)
                .setContract(contract)
                .setPrice(10)
                .setQuantity(2)
                .setExpirationTime(new DateTime("2019-09-12T12", DateTimeZone.UTC).plusDays(1));

        orderBook.submitOrder(sellOrder);

        assertFalse(orderBook.getOpenInterestForContract(contract, Direction.SHORT).isEmpty());


        var ticket = orderBook.submitOrder(buyOrder);

        assertEquals(OrderStatus.FILLED, ticket.getStatus());

        var buyerPositions = positionsService.getAccountPositions(buyer);
        var sellerPositions = positionsService.getAccountPositions(seller);

        log.debug("Positions - {}", positionsRepo.findAll());

        assertFalse(buyerPositions.isEmpty());
        assertFalse(sellerPositions.isEmpty());
    }

    @Test
    public void testNonMatch() {
        var sellOrder = new Order()
                .setAccount(seller)
                .setDirection(Direction.SHORT)
                .setContract(contract)
                .setPrice(10)
                .setQuantity(2)
                .setExpirationTime(new DateTime("2019-09-12T12", DateTimeZone.UTC).plusDays(1));

        var buyOrder = new Order()
                .setAccount(buyer)
                .setDirection(Direction.LONG)
                .setContract(contract)
                .setPrice(11)
                .setQuantity(3)
                .setExpirationTime(new DateTime("2019-09-12T12", DateTimeZone.UTC).plusDays(1));

        var sellerTicket = orderBook.submitOrder(sellOrder);
        assertEquals(OrderStatus.OPEN, sellerTicket.getStatus());

        var sellerPositions = positionsService.getAccountPositions(seller);
        assertTrue(sellerPositions.isEmpty());

        var buyerTicket = orderBook.submitOrder(buyOrder);
        assertEquals(OrderStatus.OPEN, buyerTicket.getStatus());

        var buyerPositions = positionsService.getAccountPositions(buyer);
        assertTrue(buyerPositions.isEmpty());


        assertFalse(orderBook.getOpenInterestForContract(contract, Direction.SHORT).isEmpty());
        assertFalse(orderBook.getOpenInterestForContract(contract, Direction.LONG).isEmpty());
    }

    @Test
    public void testCancelOrder() {
        var order = new Order()
                .setAccount(seller)
                .setDirection(Direction.SHORT)
                .setContract(contract)
                .setPrice(10)
                .setQuantity(2)
                .setExpirationTime(new DateTime("2019-09-12T12", DateTimeZone.UTC).plusDays(1));


        var sellerTicket = orderBook.submitOrder(order);
        assertEquals(OrderStatus.OPEN, sellerTicket.getStatus());

        assertFalse(orderBook.getOpenInterestForContract(contract, Direction.SHORT).isEmpty());

        Order cancelOrder = orderBook.cancelOrder(order);
        assertEquals(OrderStatus.CANCELLED, cancelOrder.getStatus());

        assertTrue(orderBook.getOpenInterestForContract(contract, Direction.SHORT).isEmpty());
    }
}
