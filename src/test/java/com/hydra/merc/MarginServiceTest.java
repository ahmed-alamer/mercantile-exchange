package com.hydra.merc;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.account.AccountsRepo;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.ledger.Ledger;
import com.hydra.merc.margin.MarginService;
import com.hydra.merc.margin.result.MarginResult;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created By aalamer on 08-30-2019
 */

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
        "logging.level.com.hydra=DEBUG",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.jadira.usertype.autoRegisterUserTypes=true"
})
public class MarginServiceTest {

    @Autowired
    private AccountsRepo accountsRepo;
    @Autowired
    private AccountService accountService;
    @Autowired
    private Ledger ledger;
    @Autowired
    private ContractService contractService;
    @Autowired
    private MarginService marginService;

    private ContractSpecifications contractSpecs;
    private Contract contract;

    private Account seller;
    private Account buyer;


    @Before
    public void initialize() {
        accountsRepo.save(Account.MARGINS_ACCOUNT);
        accountsRepo.save(Account.FEES_ACCOUNT);
        accountsRepo.save(Account.SETTLEMENTS_ACCOUNT);
        accountsRepo.save(Account.CASH_ACCOUNT);

        contractSpecs = new ContractSpecifications()
                .setInitialMargin(100)
                .setSymbol("SLR")
                .setTickSize(1F)
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
    public void testOpenMargin() {
        var marginResult = marginService.openMargins(contract, buyer, seller, 10f, 1);

        assertEquals(marginResult.getType(), MarginResult.Type.OPEN);
        assertEquals(marginResult.getResult().getBuyer().getCredit(), 100f, 0);
        assertEquals(marginResult.getResult().getSeller().getCredit(), 100f, 0);
    }


    @Test
    public void testCloseMargin() {
        var marginResult = marginService.openMargins(contract, buyer, seller, 10f, 1);
        var buyerMargin = marginResult.getResult().getBuyer().getMargin();


        marginService.creditMargin(buyerMargin, 20f);
        marginService.debitMargin(buyerMargin, 10f);

        var closeResult = marginService.closeMargin(buyerMargin);

        assertEquals(110, closeResult.getLedgerTransaction().getAmount(), 0);
        assertEquals(110, closeResult.getMarginTransaction().getDebit(), 0);
    }

    @Test
    public void testMarginCall() {
        var marginResult = marginService.openMargins(contract, buyer, seller, 10f, 1);
        assertEquals(marginResult.getType(), MarginResult.Type.OPEN);

        var buyerMargin = marginResult.getResult().getBuyer().getMargin();

        var settlementLeg = marginService.debitMargin(buyerMargin, 200);

        assertNotNull(settlementLeg.getMarginCall());
        assertEquals(settlementLeg.getMarginCall().getAmount(), 200f, 0);
    }
}
