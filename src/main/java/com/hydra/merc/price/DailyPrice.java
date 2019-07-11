package com.hydra.merc.price;

import com.hydra.merc.contract.Contract;
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

    private LocalDate day;

    private DateTime created = DateTime.now();
}
