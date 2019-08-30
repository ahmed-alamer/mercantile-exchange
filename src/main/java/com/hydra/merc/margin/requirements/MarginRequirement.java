package com.hydra.merc.margin.requirements;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.json.LocalDateDeserializer;
import com.hydra.merc.json.LocalDateSerializer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.LocalDate;
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

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate startDate = LocalDate.now();

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate endDate = startDate.plus(DEFAULT_REQUIREMENT_PERIOD);
}
