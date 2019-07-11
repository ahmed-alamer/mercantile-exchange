package com.hydra.merc.margin;

import com.hydra.merc.account.Account;
import com.hydra.merc.position.Position;
import lombok.Data;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@Entity
public class Margin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Position position;

    @ManyToOne
    private Account account;

    private float collateral;

    private DateTime created;

}
