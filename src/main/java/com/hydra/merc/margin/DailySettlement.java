package com.hydra.merc.margin;

import com.hydra.merc.ledger.LedgerTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public final class DailySettlement {
    private Leg longLeg;
    private Leg shortLeg;


    @Data
    @NoArgsConstructor(staticName = "create")
    @Accessors(chain = true)
    public static final class Leg {
        private Margin margin;
        private MarginTransaction marginTransaction;
        private LedgerTransaction marginCall;
    }
}
