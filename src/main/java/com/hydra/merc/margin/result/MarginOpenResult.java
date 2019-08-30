package com.hydra.merc.margin.result;

import com.hydra.merc.margin.transactions.MarginTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created By aalamer on 08-30-2019
 */
@Data
@AllArgsConstructor(staticName = "of")
public final class MarginOpenResult {
    private final MarginTransaction buyer;
    private final MarginTransaction seller;
    private float initialMargin;
}
