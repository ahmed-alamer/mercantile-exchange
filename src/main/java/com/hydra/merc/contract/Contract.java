package com.hydra.merc.contract;

import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.Period;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Contract {

    public static final Period DEFAULT_SETTLEMENT_PERIOD = Period.days(1);

    @Id
    private String symbol;

    private String name;

    private LocalDate expirationDate;
    private LocalDate issueDate;

    private int underlying;

    private float initialMargin;

    private float fee;

    private float tickSize;

    private Period settlementPeriod = DEFAULT_SETTLEMENT_PERIOD;
}
