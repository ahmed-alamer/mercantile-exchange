package com.hydra.merc.margin.transactions;

import com.hydra.merc.margin.Margin;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created By aalamer on 07-11-2019
 */
@Repository
public interface MarginTransactionsRepo extends CrudRepository<MarginTransaction, Long> {
    List<MarginTransaction> findAllByMargin(Margin margin);
}
