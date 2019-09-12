package com.hydra.merc;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountsRepo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MercantileExchangeApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(MercantileExchangeApplication.class, args);

        AccountsRepo accountsRepo = context.getBean(AccountsRepo.class);

        accountsRepo.saveAll(Account.INTERNAL_ACCOUNTS);
    }

}
