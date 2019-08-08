package com.hydra.merc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.io.Resources;
import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.account.AccountsRepo;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.margin.DailySettlement;
import com.hydra.merc.margin.MarginService;
import com.hydra.merc.position.Position;
import com.hydra.merc.position.PositionsService;
import com.hydra.merc.position.Ticket;
import com.hydra.merc.price.DailyPrice;
import com.hydra.merc.price.DailyPriceService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created By ahmed on 07-13-2019
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "logging.level.com.hydra=DEBUG",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.jadira.usertype.autoRegisterUserTypes=true"
})
public class MarginServiceTest {
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
    private MarginService marginService;

    @Before
    public void initialize() {
        accountsRepo.save(Account.MARGINS_ACCOUNT);
        accountsRepo.save(Account.FEES_ACCOUNT);
        accountsRepo.save(Account.SETTLEMENTS_ACCOUNT);
        accountsRepo.save(Account.CASH_ACCOUNT);
    }

    @Test
    public void testDailySettlement() throws IOException {
        var seller = accountService.openTradingAccount();
        var buyer = accountService.openTradingAccount();

        ledger.deposit(seller, 1000);
        ledger.deposit(buyer, 1000);

        var specifications = new ContractSpecifications()
                .setInitialMargin(100)
                .setSymbol("SLR")
                .setTickSize(0.1F)
                .setUnderlying(1);

        var contract = new Contract()
                .setFee(0.001F)
                .setExpirationDate(LocalDate.now().plusDays(30))
                .setIssueDate(LocalDate.now())
                .setSpecifications(specifications);

        contractService.createContract(specifications);
        contractService.listContract(contract);

        var price = new DailyPrice().setContract(contract).setPrice(0.9f);
        dailyPriceService.recordPrice(price);

        var position = new Position()
                .setBuyer(buyer)
                .setSeller(seller)
                .setContract(contract)
                .setOpenTime(DateTime.now())
                .setPrice(1)
                .setQuantity(2);

        var ticket = positionsService.openPosition(position);
        log.debug("Ticket: {}", JSON.writeValueAsString(ticket));

        var dailySettlement = marginService.runDailySettlement(ticket.getPosition());
        log.debug("Settlement: {}", JSON.writeValueAsString(dailySettlement));


        var expectedTicket = JSON.readValue(Resources.getResource("open-poistion-ticket.json"), Ticket.class);
        var expectedSettlement = JSON.readValue(Resources.getResource("position-daily-settlement.json"), DailySettlement.class);

        // Adjust the buyer and seller IDs
        expectedTicket.getPosition().getBuyer().setId(ticket.getPosition().getBuyer().getId());
        expectedTicket.getPosition().getSeller().setId(ticket.getPosition().getSeller().getId());

        assertEquals(expectedTicket, ticket);
        assertEquals(expectedSettlement, dailySettlement);
    }
}
