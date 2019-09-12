package com.hydra.merc.account;

import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;
import java.util.UUID;

/**
 * Created By aalamer on 07-10-2019
 */
@Data
@Entity
@Accessors(chain = true)
public class Account {

    public static final Account MARGINS_ACCOUNT = new Account()
            .setId("9d682e68-e24f-4278-a71b-78951614bbce")
            .setType(AccountType.INTERNAL);
    public static final Account SETTLEMENTS_ACCOUNT = new Account()
            .setId("2bcc8068-27d4-42ae-b693-908bd8705d9a")
            .setType(AccountType.INTERNAL);
    public static final Account FEES_ACCOUNT = new Account()
            .setId("d0eed39f-5faa-494f-9ac9-0f2149887c51")
            .setType(AccountType.INTERNAL);
    public static final Account CASH_ACCOUNT = new Account()
            .setId("a47325be-d295-4440-a829-e863fb4e974d")
            .setType(AccountType.INTERNAL);

    public static final List<Account> INTERNAL_ACCOUNTS = ImmutableList.of(
            CASH_ACCOUNT,
            FEES_ACCOUNT,
            MARGINS_ACCOUNT,
            SETTLEMENTS_ACCOUNT
    );

    @Id
    private String id = UUID.randomUUID().toString();

    private AccountType type;

}
