package com.hydra.merc.contract;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * Created By ahmed on 07-13-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class ContractSpecifications {
    public static final Period DEFAULT_SETTLEMENT_PERIOD = Period.days(1);

    @Id
    private String symbol;

    private int underlying;

    private float initialMargin;

    private float tickSize;

    @Transient
    @JsonIgnore
    private Period settlementPeriod = DEFAULT_SETTLEMENT_PERIOD;

    private String settlementPeriodRaw = writeSettlementPeriod(DEFAULT_SETTLEMENT_PERIOD);

    private static String writeSettlementPeriod(Period settlementPeriod) {
        return ISOPeriodFormat.standard().print(settlementPeriod);
    }

    @Transient
    @JsonIgnore
    public Period getSettlementPeriod() {
        return ISOPeriodFormat.standard().parsePeriod(settlementPeriodRaw);
    }

    public ContractSpecifications setSettlementPeriod(Period period) {
        this.settlementPeriod = period;
        this.settlementPeriodRaw = writeSettlementPeriod(settlementPeriod);

        return this;
    }


}
