package com.hydra.merc.contract;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Created By aalamer on 07-16-2019
 */
@RestController
@RequestMapping("/contracts")
public class ContractsController {

    private final ContractService contractService;

    @Autowired
    public ContractsController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/specs")
    public List<ContractSpecifications> getAllSpecs() {
        return contractService.getAllContractSpecs();
    }

    @GetMapping("/contract/{symbol}")
    public ResponseEntity<List<Contract>> getListedContracts(@PathVariable("symbol") String symbol) {
        Optional<ContractSpecifications> contractSpecs = contractService.getContractSpecs(symbol);
        if (contractSpecs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var contracts = contractService.getListedContracts(contractSpecs.get());

        return ResponseEntity.ok(contracts);
    }

    @PostMapping("/create")
    public ContractSpecifications createContract(@RequestBody ContractSpecifications specifications) {
        return contractService.createContract(specifications);
    }

    @PostMapping("/list")
    public Contract listContract(@RequestBody Contract contract) {
        return contractService.listContract(contract);
    }
}
