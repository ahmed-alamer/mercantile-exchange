package com.hydra.merc.account;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Account {

    public static final Account MARGINS_ACCOUNT = new Account().setType(AccountType.INTERNAL);
    public static final Account SETTLEMENTS_ACCOUNT = new Account().setType(AccountType.INTERNAL);
    public static final Account FEES_ACCOUNT = new Account().setType(AccountType.INTERNAL);

    @Id
    private String id = UUID.randomUUID().toString();

    private AccountType type;

}
