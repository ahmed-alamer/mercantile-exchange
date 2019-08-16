package com.hydra.merc.price;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.json.DateTimeDeserializer;
import com.hydra.merc.json.DateTimeSerializer;
import com.hydra.merc.json.LocalDateDeserializer;
import com.hydra.merc.json.LocalDateSerializer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import javax.persistence.*;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class DailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private float price;

    @ManyToOne
    private Contract contract;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate day = LocalDate.now();

    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private DateTime created = DateTime.now();
}
