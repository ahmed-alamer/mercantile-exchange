package com.hydra.merc.contract;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.LocalDateSerializer;
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

    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate expirationDate;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate issueDate;

    private float fee;

}
