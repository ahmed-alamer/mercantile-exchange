package com.hydra.merc.contract;

import lombok.Data;
import lombok.experimental.Accessors;
import org.joda.time.LocalDate;

import javax.persistence.*;


/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private ContractSpecifications specifications;

    private LocalDate expirationDate;
    private LocalDate issueDate;

    private float fee;

}
