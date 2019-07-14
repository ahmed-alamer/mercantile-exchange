package com.hydra.merc.ledger;

import com.hydra.merc.account.Account;
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
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Account credit;

    @ManyToOne
    private Account debit;

    private float amount;

    private DateTime timestamp = DateTime.now();
}
