package com.hydra.merc.margin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hydra.merc.account.Account;
import com.hydra.merc.contract.Contract;
import com.hydra.merc.json.DateTimeDeserializer;
import com.hydra.merc.json.DateTimeSerializer;
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
    private Account account;

    @ManyToOne
    private Contract contract;

    private float price;
    private int quantity;

    private float collateral;

    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private DateTime created = DateTime.now();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getAccountId() {
        return account.getId();
    }

}
