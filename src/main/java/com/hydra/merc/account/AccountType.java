package com.hydra.merc.account;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Created By aalamer on 07-10-2019
 */
public enum AccountType {
    TRADING,
    INTERNAL;

    public static Optional<AccountType> parse(String value) {
        for (AccountType accountType : values()) {
            if (StringUtils.equalsIgnoreCase(accountType.name(), value)) {
                return Optional.of(accountType);
            }
        }

        return Optional.empty();
    }
}
