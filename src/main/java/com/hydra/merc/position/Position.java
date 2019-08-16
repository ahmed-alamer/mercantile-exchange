package com.hydra.merc.position;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.json.DateTimeDeserializer;
import com.hydra.merc.json.DateTimeSerializer;
import com.hydra.merc.margin.Margin;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Contract contract;

    @ManyToOne
    private Margin seller;

    @ManyToOne
    private Margin buyer;

    private float price;
    private int quantity;

    private float collateral;

    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private DateTime openTime = DateTime.now();

    @Enumerated(EnumType.STRING)
    private PositionType type; // TODO

    private boolean open = true;

}
