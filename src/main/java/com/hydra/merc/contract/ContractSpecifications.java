package com.hydra.merc.contract;

import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.Period;

import javax.persistence.Entity;
import javax.persistence.Id;

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

    private Period settlementPeriod = DEFAULT_SETTLEMENT_PERIOD;

}
