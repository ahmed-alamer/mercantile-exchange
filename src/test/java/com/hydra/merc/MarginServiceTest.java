package com.hydra.merc;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.contract.ContractSpecifications;
import com.hydra.merc.contract.ContractSpecificationsRepo;
import com.hydra.merc.contract.ContractsRepo;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.ledger.LedgerTransactionsRepo;
import com.hydra.merc.margin.*;
import com.hydra.merc.position.Position;
import com.hydra.merc.price.DailyPrice;
import com.hydra.merc.price.DailyPriceRepo;
import com.hydra.merc.utils.*;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created By ahmed on 07-13-2019
 */
@RunWith(SpringRunner.class)
public class MarginServiceTest {

    @TestConfiguration
    private static final class Config {
        private final EntityManager<ContractSpecifications, String> contractSpecsManager = new EntityManager<>(StringIdHandler.of(ContractSpecifications::getSymbol));
        private final EntityManager<Contract, Long> contractEntityManager = new GeneratedIdEntityManager<>(LongIdHandler.of(Contract::setId, Contract::getId), LongIdGenerator.create());
        private final EntityManager<Margin, Long> marginEntityManager = new GeneratedIdEntityManager<>(LongIdHandler.of(Margin::setId, Margin::getId), LongIdGenerator.create());
        private final EntityManager<MarginRequirement, Long> marginRequirementEntityManager = new GeneratedIdEntityManager<>(LongIdHandler.of(MarginRequirement::setId, MarginRequirement::getId), LongIdGenerator.create());
        private final EntityManager<MarginTransaction, Long> marginTransactionEntityManager = new GeneratedIdEntityManager<>(LongIdHandler.of(MarginTransaction::setId, MarginTransaction::getId), LongIdGenerator.create());
        private final EntityManager<LedgerTransaction, Long> ledgerTransactionEntityManager = new GeneratedIdEntityManager<>(LongIdHandler.of(LedgerTransaction::setId, LedgerTransaction::getId), LongIdGenerator.create());
        private final EntityManager<DailyPrice, Long> dailyPriceEntityManager = new GeneratedIdEntityManager<>(LongIdHandler.of(DailyPrice::setId, DailyPrice::getId), LongIdGenerator.create());

        private final MarginsRepo marginsRepo = mock(MarginsRepo.class);
        private final MarginRequirementsRepo marginRequirementsRepo = mock(MarginRequirementsRepo.class);
        private final MarginTransactionsRepo marginTransactionsRepo = mock(MarginTransactionsRepo.class);
        private final LedgerTransactionsRepo ledgerTransactionsRepo = mock(LedgerTransactionsRepo.class);
        private final DailyPriceRepo dailyPriceRepo = mock(DailyPriceRepo.class);
        private final ContractSpecificationsRepo contractSpecificationsRepo = mock(ContractSpecificationsRepo.class);
        private final ContractsRepo contractsRepo = mock(ContractsRepo.class);

        public void initialize() {
            when(marginsRepo.save(any(Margin.class))).thenAnswer(invocationOnMock -> marginEntityManager.save(invocationOnMock.getArgument(0)));
            when(marginsRepo.findAllByPosition(any(Position.class))).thenAnswer(invocationOnMock -> {
                Position position = invocationOnMock.getArgument(0);
                return marginEntityManager.getTable()
                        .values()
                        .stream()
                        .filter(margin -> margin.getPosition().getId() == position.getId())
                        .collect(Collectors.toList());
            });

            when(marginRequirementsRepo.save(any(MarginRequirement.class))).thenAnswer(invocationOnMock -> {
                MarginRequirement marginRequirement = invocationOnMock.getArgument(0);

                return marginRequirementEntityManager.save(marginRequirement);
            });

            when(marginRequirementsRepo.findByContractAndStartAfterAndEndBeforeOrderByStartDesc(any(Contract.class), any(DateTime.class), any(DateTime.class))).thenAnswer(invocationOnMock -> {
                Contract contract = invocationOnMock.getArgument(0);
                DateTime start = invocationOnMock.getArgument(1);
                DateTime end = invocationOnMock.getArgument(1);

                return marginRequirementEntityManager.getTable()
                        .values()
                        .stream()
                        .filter(marginRequirement -> marginRequirement.getContract().getId() == contract.getId())
                        .filter(marginRequirement -> marginRequirement.getStart().isAfter(start) && marginRequirement.getEnd().isBefore(end))
                        .findFirst();
            });

            when(marginTransactionsRepo.save(any(MarginTransaction.class))).thenAnswer(invocationOnMock -> marginTransactionEntityManager.save(invocationOnMock.getArgument(0)));
            when(marginTransactionsRepo.findAllByMargin(any(Margin.class))).thenAnswer(invocationOnMock -> {
                Margin margin = invocationOnMock.getArgument(0);
                return marginTransactionEntityManager.getTable()
                        .values()
                        .stream()
                        .filter(marginTransaction -> marginTransaction.getMargin().getId() == margin.getId())
                        .collect(Collectors.toList());
            });

            when(ledgerTransactionsRepo.save(any(LedgerTransaction.class))).thenAnswer(invocationOnMock -> {
                LedgerTransaction transaction = invocationOnMock.getArgument(0);

                return ledgerTransactionEntityManager.save(transaction);
            });

            when(ledgerTransactionsRepo.findAllByCredit(any(Account.class))).thenAnswer(invocationOnMock -> {
                Account account = invocationOnMock.getArgument(0);

                return ledgerTransactionEntityManager.getTable()
                        .values()
                        .stream()
                        .filter(ledgerTransaction -> Objects.equals(ledgerTransaction.getCredit().getId(), account.getId()))
                        .collect(Collectors.toList());
            });

            when(ledgerTransactionsRepo.findAllByDebit(any(Account.class))).thenAnswer(invocationOnMock -> {
                Account account = invocationOnMock.getArgument(0);

                return ledgerTransactionEntityManager.getTable()
                        .values()
                        .stream()
                        .filter(ledgerTransaction -> Objects.equals(ledgerTransaction.getDebit().getId(), account.getId()))
                        .collect(Collectors.toList());
            });

            when(dailyPriceRepo.save(any(DailyPrice.class))).thenAnswer(invocationOnMock -> {
                DailyPrice dailyPrice = invocationOnMock.getArgument(0);

                return dailyPriceEntityManager.save(dailyPrice);
            });

            when(dailyPriceRepo.findContractAndDay(any(Contract.class), any(LocalDate.class))).thenAnswer(invocationOnMock -> {
                Contract contract = invocationOnMock.getArgument(0);
                LocalDate day = invocationOnMock.getArgument(1);


                return dailyPriceEntityManager.getTable()
                        .values()
                        .stream()
                        .filter(dailyPrice -> dailyPrice.getContract().getId() == contract.getId())
                        .filter(dailyPrice -> dailyPrice.getDay().equals(day))
                        .findFirst();
            });

            when(contractSpecificationsRepo.save(any(ContractSpecifications.class))).thenAnswer(invocationOnMock -> {
                ContractSpecifications specifications = invocationOnMock.getArgument(0);

                return contractSpecsManager.save(specifications);
            });

            when(contractsRepo.save(any(Contract.class))).thenAnswer(invocationOnMock -> contractEntityManager.save(invocationOnMock.getArgument(0)));
            when(contractsRepo.findById(anyLong())).thenAnswer(invocationOnMock -> {
                long id = invocationOnMock.getArgument(0);
                return Optional.ofNullable(contractEntityManager.getTable().get(id));
            });

        }
    }

}
