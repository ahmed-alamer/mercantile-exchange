package com.hydra.merc.margin;

import com.hydra.merc.contract.Contract;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.persistence.*;

/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class MarginRequirement {

    public static final Period DEFAULT_REQUIREMENT_PERIOD = Period.weeks(1);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    private Contract contract;

    private float initialMargin;

    private DateTime start = DateTime.now();
    private DateTime end = start.plus(DEFAULT_REQUIREMENT_PERIOD);
}
