package com.hydra.merc;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.account.AccountsRepo;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.ledger.Ledger;
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
 * Created By aalamer on 10-21-2019
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
public class LedgerTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private Ledger ledger;

    @Autowired
    private AccountsRepo accountsRepo;


    @Before
    public void initialize() {
        accountsRepo.saveAll(Account.INTERNAL_ACCOUNTS);
    }

    @Test
    public void testDeposit() {
        var account = accountService.openTradingAccount();
        log.debug("Account: {}", account);

        var depositAmount = 1000f;

        var deposit = ledger.deposit(account, depositAmount);
        assertEquals(deposit.getCredit(), account);

        var balance = ledger.getAccountBalance(account);
        assertEquals(balance, 1000f, 0);

        var internalCashAccountBalance = ledger.getAccountBalance(Account.CASH_ACCOUNT);
        assertEquals(internalCashAccountBalance, -1000f, 0);
    }

    @Test
    public void testDebitFees() {
        var buyer = accountService.openTradingAccount();
        var seller = accountService.openTradingAccount();

        var depositAmount = 1000f;

        var contractSpecs = new ContractSpecifications()
                .setInitialMargin(100)
                .setSymbol("SLR")
                .setTickSize(1F)
                .setUnderlying(1);

        var contract = new Contract()
                .setFee(0.001F)
                .setExpirationDate(LocalDate.now().plusDays(30))
                .setIssueDate(LocalDate.now())
                .setSpecifications(contractSpecs);

        contractService.createContract(contractSpecs);
        contractService.listContract(contract);

        ledger.deposit(buyer, depositAmount);
        ledger.deposit(seller, depositAmount);

        var fees = ledger.debitFees(contract, buyer, seller, 10, 0.01f);

        var buyerFee = fees.getBuyer();
        assertEquals(buyerFee.getDebit(), buyer);
        assertEquals(buyerFee.getAmount(), 0.099999994f, 0);

        var sellerFee = fees.getSeller();
        assertEquals(sellerFee.getDebit(), seller);
        assertEquals(sellerFee.getAmount(), 0.099999994f, 0);

        var feeAccountBalance = ledger.getAccountBalance(Account.FEES_ACCOUNT);
        assertEquals(feeAccountBalance, 0.099999994f * 2f, 0);
    }

    @Test
    public void testDebitMargin() {
        var buyer = accountService.openTradingAccount();
        var seller = accountService.openTradingAccount();

        var depositAmount = 1000f;

        ledger.deposit(buyer, depositAmount);
        ledger.deposit(seller, depositAmount);

        var debitMargin = ledger.debitMargin(buyer, seller, 250f);

        var buyerFee = debitMargin.getBuyer();
        assertEquals(buyerFee.getDebit(), buyer);
        assertEquals(buyerFee.getAmount(), 250f, 0);

        var sellerFee = debitMargin.getSeller();
        assertEquals(sellerFee.getDebit(), seller);
        assertEquals(sellerFee.getAmount(), 250f, 0);

        var marginAccountBalance = ledger.getAccountBalance(Account.MARGINS_ACCOUNT);
        assertEquals(250f * 2f, marginAccountBalance, 0);
    }
}
