package com.hydra.merc.margin.result;

import com.hydra.merc.account.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created By aalamer on 08-30-2019
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public final class MarginOpenError {
    private List<Account> accounts;
    private String message;
}
