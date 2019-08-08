package com.hydra.merc.margin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hydra.merc.account.Account;
import com.hydra.merc.position.Position;
import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Created By aalamer on 07-11-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Margin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JsonIgnore
    private Position position;

    @ManyToOne
    @JsonIgnore
    private Account account;

    private float collateral;

    private DateTime created = DateTime.now();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public long getPositionId() {
        return position.getId();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getAccountId() {
        return account.getId();
    }

}
