package com.hydra.merc.order;

import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Created By aalamer on 07-15-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Contract contract;

    @ManyToOne
    private Account account;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private int quantity;

    private DateTime expirationTime;

    public enum Direction {
        LONG {
            @Override
            public Direction getAnte() {
                return SHORT;
            }
        },
        SHORT {
            @Override
            public Direction getAnte() {
                return LONG;
            }
        };

        public abstract Direction getAnte();
    }
}
