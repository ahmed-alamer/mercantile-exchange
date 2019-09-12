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
import com.hydra.merc.position.PositionsService;
import com.hydra.merc.price.DailyPrice;
import com.hydra.merc.price.DailyPriceService;
import com.hydra.merc.settlement.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created By ahmed on 07-13-2019
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
public class SettlementServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JodaModule());

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private AccountService accountService;

    @Autowired
    private Ledger ledger;

    @Autowired
    private ContractService contractService;

    @Autowired
    private DailyPriceService dailyPriceService;

    @Autowired
    private PositionsService positionsService;

    @Autowired
    private SettlementService settlementService;

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
    public void testDailySettlementPriceDown() throws IOException {
        var ticket = positionsService.openPosition(contract, buyer, seller, 1.2f, 2);
        log.debug("Ticket: {}", JSON.writeValueAsString(ticket));

        assertEquals(ticket.getBuyer().getMarginTransaction().getCredit(), 200f, 0);
        assertEquals(ticket.getSeller().getMarginTransaction().getCredit(), 200f, 0);

        assertEquals(ticket.getBuyer().getLedgerTransaction().getAmount(), 200f, 0);
        assertEquals(ticket.getBuyer().getLedgerTransaction().getDebit(), buyer);

        assertEquals(ticket.getSeller().getLedgerTransaction().getAmount(), 200f, 0);
        assertEquals(ticket.getSeller().getLedgerTransaction().getDebit(), seller);


        // Price goes down
        var price = new DailyPrice().setContract(contract).setPrice(1f);
        dailyPriceService.recordPrice(price);

        var settlement = settlementService.settle(ticket.getPosition());
        log.debug("Settlement: {}", JSON.writeValueAsString(settlement));

        assertEquals(settlement.getLongLeg().getMarginTransaction().getCredit(), 0.4000001f, 0);
        assertEquals(settlement.getShortLeg().getMarginTransaction().getDebit(), 0.4000001f, 0);

        assertNull(settlement.getShortLeg().getMarginCall());
        assertNull(settlement.getLongLeg().getMarginCall());
    }


    @Test
    public void testDailySettlementPriceUp() throws IOException {
        var ticket = positionsService.openPosition(contract, buyer, seller, 1f, 2);
        log.debug("Ticket: {}", JSON.writeValueAsString(ticket));

        assertEquals(ticket.getBuyer().getMarginTransaction().getCredit(), 200f, 0);
        assertEquals(ticket.getSeller().getMarginTransaction().getCredit(), 200f, 0);

        // Price goes uo
        var price = new DailyPrice().setContract(contract).setPrice(1.2f);
        dailyPriceService.recordPrice(price);

        var settlement = settlementService.settle(ticket.getPosition());
        log.debug("Settlement: {}", JSON.writeValueAsString(settlement));

        assertEquals(settlement.getShortLeg().getMarginTransaction().getDebit(), 0.4000001f, 0);
        assertEquals(settlement.getLongLeg().getMarginTransaction().getCredit(), 0.4000001f, 0);

        assertNull(settlement.getShortLeg().getMarginCall());
        assertNull(settlement.getLongLeg().getMarginCall());
    }
}
