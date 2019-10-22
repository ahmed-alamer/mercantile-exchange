package com.hydra.merc;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.account.AccountsRepo;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.position.PositionsService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * Created By aalamer on 10-22-2019
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PositionServiceTest {


    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private AccountService accountService;

    @Autowired
    private Ledger ledger;

    @Autowired
    private ContractService contractService;

    @Autowired
    private PositionsService positionsService;

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
    public void testOpenPosition() {
        var ticket = positionsService.openPosition(contract, buyer, seller, 1.2f, 2);

        assertEquals(ticket.getBuyer().getMarginTransaction().getCredit(), 200f, 0);
        assertEquals(ticket.getSeller().getMarginTransaction().getCredit(), 200f, 0);

        assertEquals(ticket.getBuyer().getLedgerTransaction().getAmount(), 200f, 0);
        assertEquals(ticket.getBuyer().getLedgerTransaction().getDebit(), buyer);

        assertEquals(ticket.getSeller().getLedgerTransaction().getAmount(), 200f, 0);
        assertEquals(ticket.getSeller().getLedgerTransaction().getDebit(), seller);
    }

    @Test
    public void testClosePosition() {
        var openTicket = positionsService.openPosition(contract, buyer, seller, 1.2f, 2);

        var closeTicket = positionsService.closePosition(openTicket.getPosition());

        assertEquals(closeTicket.getBuyer().getMarginTransaction().getDebit(), 200f, 0);
        assertEquals(closeTicket.getSeller().getMarginTransaction().getDebit(), 200f, 0);

        assertEquals(closeTicket.getBuyer().getLedgerTransaction().getAmount(), 200f, 0);
        assertEquals(closeTicket.getBuyer().getLedgerTransaction().getCredit(), buyer);

        assertEquals(closeTicket.getSeller().getLedgerTransaction().getAmount(), 200f, 0);
        assertEquals(closeTicket.getSeller().getLedgerTransaction().getCredit(), seller);
    }
}
