package com.hydra.merc.order;

import com.hydra.merc.contract.Contract;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created By aalamer on 09-12-2019
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class OpenInterest {
    private Contract contract;
    private Order order;
}
