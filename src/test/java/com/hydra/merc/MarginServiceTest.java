package com.hydra.merc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.account.AccountsRepo;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.margin.MarginService;
import com.hydra.merc.position.Position;
import com.hydra.merc.position.PositionsService;
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

/**
 * Created By ahmed on 07-13-2019
 */
@Slf4j
//@DataJpaTest
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "logging.level.com.hydra=DEBUG",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.jadira.usertype.autoRegisterUserTypes=true"
})
public class MarginServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JodaModule());
    ;

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
    public void testWiring() throws JsonProcessingException {
        var seller = accountService.openTradingAccount(new Account());
        var buyer = accountService.openTradingAccount(new Account());

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
        log.debug(String.format("Ticket: %s", JSON.writeValueAsString(ticket)));

        var dailySettlement = marginService.runDailySettlement(ticket.getPosition());

        log.debug("Settlement: {}", JSON.writeValueAsString(dailySettlement));

    }
}
