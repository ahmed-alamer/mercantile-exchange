package com.hydra.merc.margin;

import com.hydra.merc.account.Account;
import com.hydra.merc.account.AccountService;
import com.hydra.merc.exceptions.AccountNotFound;
import com.hydra.merc.exceptions.PositionNotFound;
import com.hydra.merc.position.Position;
import com.hydra.merc.position.PositionsService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created By aalamer on 07-18-2019
 */
@RestController
@RequestMapping("/margin")
public class MarginController {

    private final AccountService accountService;
    private final PositionsService positionsService;
    private final MarginService marginService;

    @Autowired
    public MarginController(AccountService accountService, PositionsService positionsService, MarginService marginService) {
        this.accountService = accountService;
        this.positionsService = positionsService;
        this.marginService = marginService;
    }


    @GetMapping("/{accountId}/{positionId}/transactions")
    public ResponseEntity getTransactions(@PathVariable("accountId") String accountId, @PathVariable("positionId") long positionId) throws AccountNotFound, PositionNotFound {
        var account = getAccount(accountId);
        var position = getPosition(positionId);

        var transactions = marginService.getMarginTransactions(account, position);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{accountId}/{positionId}/balance")
    public ResponseEntity<MarginBalance> getMarginBalance(@PathVariable("accountId") String accountId, @PathVariable("positionId") long positionId) throws AccountNotFound, PositionNotFound {
        var account = getAccount(accountId);
        var position = getPosition(positionId);

        var balance = marginService.getMarginBalance(account, position);

        return ResponseEntity.ok(MarginBalance.of(accountId, positionId, balance));
    }

    private Position getPosition(long positionId) throws PositionNotFound {
        var positionById = positionsService.findById(positionId);
        if (positionById.isEmpty()) {
            throw new PositionNotFound(positionId);
        }
        return positionById.get();
    }

    private Account getAccount(String accountId) throws AccountNotFound {
        var accountById = accountService.findById(accountId);
        if (accountById.isEmpty()) {
            throw new AccountNotFound(accountId);
        }
        return accountById.get();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class MarginBalance {
        private String accountId;
        private long positionId;
        private float balance;
    }
}
