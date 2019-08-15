package com.hydra.merc.position;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import com.hydra.merc.account.Account;
import com.hydra.merc.json.DateTimeDeserializer;
import com.hydra.merc.json.DateTimeSerializer;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.margin.MarginTransaction;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@Accessors(chain = true)
public final class Ticket {
    private PositionsService.TicketType type;
    private Position position;

    private List<LedgerTransaction> transactions = Lists.newArrayList();
    private List<MarginTransaction> marginTransactions = Lists.newArrayList();

    private List<Account> failedAccounts;

    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private DateTime timestamp = DateTime.now();


    public Ticket addTransactions(List<LedgerTransaction> transactions) {
        this.transactions.addAll(transactions);
        return this;
    }

    public Ticket addMarinTransactions(List<MarginTransaction> transactions) {
        this.marginTransactions.addAll(transactions);
        return this;
    }
}
