package com.hydra.merc.margin;

import com.hydra.merc.ledger.LedgerTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public final class DailySettlement {
    private Margin buyer;
    private Margin seller;
    private List<LedgerTransaction> ledgerTransactions;
    private List<MarginTransaction> marginTransactions;
}
