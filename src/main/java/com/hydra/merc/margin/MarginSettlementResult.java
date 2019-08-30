package com.hydra.merc.margin;

import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.margin.transactions.MarginTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created By aalamer on 08-21-2019
 */
@Data
@AllArgsConstructor(staticName = "of")
public final class MarginSettlementResult {
    private final MarginTransaction marginTransaction;
    private final LedgerTransaction ledgerTransaction;
}
