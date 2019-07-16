package com.hydra.merc.fee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created By aalamer on 07-16-2019
 */

@RestController
@RequestMapping("/fees")
public class FeeController {

    private final FeesService feesService;

    @Autowired
    public FeeController(FeesService feesService) {
        this.feesService = feesService;
    }

    @GetMapping
    public List<ContractFee> getFees() {
        return feesService.all();
    }
}
