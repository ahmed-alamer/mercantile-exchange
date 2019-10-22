package com.hydra.merc;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractService;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.margin.Margin;
import com.hydra.merc.margin.MarginsRepo;
import com.hydra.merc.margin.transactions.MarginLedger;
import com.hydra.merc.margin.transactions.MarginTransaction;
import com.hydra.merc.margin.transactions.MarginTransactionType;
import com.hydra.merc.settlement.Settlement;
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
import static org.junit.Assert.assertNull;

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
public class MarginLedgerTest {

    @Autowired
    private MarginLedger marginLedger;

    @Autowired
    private ContractService contractService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MarginsRepo marginsRepo;

    private Account account;

    private Margin margin;


    @Before
    public void initialize() {
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

        account = accountService.openTradingAccount();

        margin = new Margin()
                .setCollateral(100)
                .setPrice(10)
                .setContract(contract)
                .setQuantity(10)
                .setAccount(account)
                .setId(1);

        marginsRepo.save(margin);
    }

    @Test
    public void testDebitMargin() {
        Settlement.Leg debit = marginLedger.debit(margin, 100);

        assertNull(debit.getMarginCall());
        assertEquals(MarginTransactionType.SETTLEMENT, debit.getMarginTransaction().getType());
        assertEquals(100, debit.getMarginTransaction().getDebit(), 0);
        assertEquals(0, debit.getMarginTransaction().getCredit(), 0);
    }

    @Test
    public void testCreditMargin() {
        Settlement.Leg credit = marginLedger.credit(margin, 100);

        assertNull(credit.getMarginCall());
        assertEquals(MarginTransactionType.SETTLEMENT, credit.getMarginTransaction().getType());
        assertEquals(0, credit.getMarginTransaction().getDebit(), 0);
        assertEquals(100, credit.getMarginTransaction().getCredit(), 0);
    }

    @Test
    public void testMarginCall() {
        MarginTransaction marginTransaction = marginLedger.issueMarginCall(margin, 100);

        assertEquals(100, marginTransaction.getDebit(), 0);
        assertEquals(0, marginTransaction.getCredit(), 0);
        assertEquals(MarginTransactionType.MARGIN_CALL, marginTransaction.getType());
    }

    @Test
    public void testOpenMargin() {
        var transaction = marginLedger.openMargin(margin, 100f);

        assertEquals(MarginTransactionType.OPEN, transaction.getType());
        assertEquals(100f, transaction.getCredit(), 0);
    }

    @Test
    public void testCloseMarginWithPositiveBalance() {
        marginLedger.openMargin(margin, 100f);

        var transaction = marginLedger.closeMargin(margin);

        assertEquals(transaction.getType(), MarginTransactionType.CLOSE);
        assertEquals(transaction.getDebit(), 100f, 0);

        float balance = marginLedger.getBalance(margin);
        assertEquals(0, balance, 0);
    }

    @Test
    public void testCloseMarginWithDelinquentBalance() {
        marginLedger.openMargin(margin, 100f);
        marginLedger.debit(margin, 200f);

        float balanceBeforeClose = marginLedger.getBalance(margin);
        log.debug("Balance before close: {}", balanceBeforeClose);

        MarginTransaction transaction = marginLedger.closeMargin(margin);
        assertEquals(MarginTransactionType.CLOSE, transaction.getType());
        assertEquals(100f, transaction.getCredit(), 0);

        float balance = marginLedger.getBalance(margin);
        assertEquals(0, balance, 0);
    }
}
