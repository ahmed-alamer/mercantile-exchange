package com.hydra.merc.position;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
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
    private Account seller;

    @ManyToOne
    private Account buyer;

    private float price;
    private int quantity;

    private DateTime openTime = DateTime.now();

    @Enumerated(EnumType.STRING)
    private PositionType type;

    private boolean open = true;

}
