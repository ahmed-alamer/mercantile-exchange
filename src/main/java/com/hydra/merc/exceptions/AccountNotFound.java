package com.hydra.merc.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created By aalamer on 07-19-2019
 */

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Account was not found")
public class AccountNotFound extends Exception {
    public AccountNotFound(String accountId) {
        super(String.format("Account %s was not found", accountId));
    }
}
