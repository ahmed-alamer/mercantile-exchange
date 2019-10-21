package com.hydra.merc.position;

import com.hydra.merc.account.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Repository
public interface PositionsRepo extends CrudRepository<Position, Long> {

    List<Position> findAllByOpenEquals(boolean open);

    List<Position> findAllByBuyerAccount(Account account);

    List<Position> findAllBySellerAccount(Account account);

}
