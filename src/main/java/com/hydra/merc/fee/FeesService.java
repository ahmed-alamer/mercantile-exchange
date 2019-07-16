package com.hydra.merc.fee;

import com.google.common.collect.Lists;
import com.hydra.merc.contract.Contract;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created By aalamer on 07-10-2019
 */
@Service
public class FeesService {

    private final ContractFeesRepo contractFeesRepo;

    @Autowired
    public FeesService(ContractFeesRepo contractFeesRepo) {
        this.contractFeesRepo = contractFeesRepo;
    }

    public float getContractFee(Contract contract) {
        return contractFeesRepo.findByContractAndEndBeforeOrderByStartDesc(contract, DateTime.now())
                .map(ContractFee::getFee)
                .orElse(contract.getFee());
    }

    public List<ContractFee> all() {
        return Lists.newArrayList(contractFeesRepo.findAll());
    }
}
