package com.hydra.merc.contract;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Created By aalamer on 07-15-2019
 */
@Service
public class ContractService {

    private final ContractsRepo contractsRepo;
    private final ContractSpecificationsRepo contractSpecificationsRepo;

    @Autowired
    public ContractService(ContractsRepo contractsRepo,
                           ContractSpecificationsRepo contractSpecificationsRepo) {
        this.contractsRepo = contractsRepo;
        this.contractSpecificationsRepo = contractSpecificationsRepo;
    }


    public ContractSpecifications createContract(ContractSpecifications specs) {
        return contractSpecificationsRepo.save(specs);
    }

    public Contract listContract(Contract contract) {
        return contractsRepo.save(contract);
    }

    public Optional<ContractSpecifications> getContractSpecs(String id) {
        return contractSpecificationsRepo.findById(id);
    }

    public List<ContractSpecifications> getAllContractSpecs() {
        return Lists.newArrayList(contractSpecificationsRepo.findAll());
    }

    public List<Contract> getListedContracts(ContractSpecifications specifications) {
        return contractsRepo.findAllBySpecifications(specifications);
    }
}
