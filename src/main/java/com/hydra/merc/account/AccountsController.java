package com.hydra.merc.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created By aalamer on 07-16-2019
 */

@RestController
@RequestMapping("/accounts")
public class AccountsController {

    private final AccountService accountService;

    @Autowired
    public AccountsController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/create")
    public Account openAccount() {
        return accountService.openTradingAccount();
    }

    @GetMapping("/all")
    public List<Account> getAccounts() {
        return accountService.all();
    }
}
