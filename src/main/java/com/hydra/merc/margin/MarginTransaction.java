package com.hydra.merc.margin;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class MarginTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Margin margin;

    private float debit;
    private float credit;


    @Enumerated(EnumType.STRING)
    private MarginTransactionType type;

}
