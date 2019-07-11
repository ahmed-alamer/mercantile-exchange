package com.hydra.merc.transaction;

import com.hydra.merc.account.Account;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.UUID;

/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id = UUID.randomUUID().toString();

    @ManyToOne
    private Account credit;

    @ManyToOne
    private Account debit;

    private float amount;

    private DateTime timestamp = DateTime.now();
}
