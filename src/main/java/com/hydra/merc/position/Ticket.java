package com.hydra.merc.position;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hydra.merc.json.DateTimeDeserializer;
import com.hydra.merc.json.DateTimeSerializer;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.margin.transactions.MarginTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class Ticket {

    private Position position;
    private PositionsService.TicketType type;

    private Leg buyer;
    private Leg seller;

    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private DateTime timestamp = DateTime.now();

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    @AllArgsConstructor(staticName = "of")
    public static final class Leg {
        private MarginTransaction marginTransaction;
        private LedgerTransaction ledgerTransaction;
        private LedgerTransaction fee;

        public static Leg of(MarginTransaction marginTransaction, LedgerTransaction ledgerTransaction) {
            return new Leg()
                    .setMarginTransaction(marginTransaction)
                    .setLedgerTransaction(ledgerTransaction);
        }
    }

}
