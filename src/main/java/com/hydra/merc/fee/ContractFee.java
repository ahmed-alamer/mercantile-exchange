package com.hydra.merc.fee;

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
public class ContractFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Contract contract;

    private float fee;

    private DateTime start;
    private DateTime end;


}
