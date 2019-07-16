package com.hydra.merc.order;

import com.hydra.merc.contract.Contract;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created By aalamer on 07-15-2019
 */
@Repository
public interface OrdersRepo extends CrudRepository<Order, Long> {

    List<Order> findAllByContract(Contract contract);
}
