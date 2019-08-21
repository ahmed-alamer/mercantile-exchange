package com.hydra.merc.margin;

import com.hydra.merc.account.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created By aalamer on 08-21-2019
 */
public interface MarginResult {
    Type getType();

    enum Type {
        SETTLEMENT,
        OPEN,
        INSUFFICIENT_FUNDS;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    final class InsufficientFundForMargin implements MarginResult {
        private List<Account> accounts;

        @Override
        public Type getType() {
            return Type.INSUFFICIENT_FUNDS;
        }
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    final class MarginSettlement implements MarginResult {
        private final MarginSettlementResult buyer;
        private final MarginSettlementResult seller;

        @Override
        public Type getType() {
            return Type.SETTLEMENT;
        }
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    final class MarginOpenResult implements MarginResult {
        private final MarginTransaction buyer;
        private final MarginTransaction seller;
        private float initialMargin;

        @Override
        public Type getType() {
            return Type.OPEN;
        }
    }
}
