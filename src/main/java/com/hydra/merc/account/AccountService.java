package com.hydra.merc.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created By ahmed on 07-13-2019
 */
@Service
public class AccountService {

    private final AccountsRepo accountsRepo;

    @Autowired
    public AccountService(AccountsRepo accountsRepo) {
        this.accountsRepo = accountsRepo;
    }

    public Account openTradingAccount(Account account) {
        account.setType(AccountType.TRADING);

        return accountsRepo.save(account);
    }
}
