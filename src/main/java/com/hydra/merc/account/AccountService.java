package com.hydra.merc.account;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Account openTradingAccount() {
        var account = new Account().setType(AccountType.TRADING);

        return accountsRepo.save(account);
    }

    public List<Account> all() {
        return Lists.newArrayList(accountsRepo.findAll());
    }

    public Optional<Account> findById(String accountId) {
        return accountsRepo.findById(accountId);
    }
}
