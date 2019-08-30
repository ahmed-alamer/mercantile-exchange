package com.hydra.merc.margin.result;

import com.hydra.merc.margin.MarginSettlementResult;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created By aalamer on 08-30-2019
 */
@Data
@AllArgsConstructor(staticName = "of")
public final class MarginSettlement {
    private final MarginSettlementResult buyer;
    private final MarginSettlementResult seller;
}
