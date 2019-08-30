package com.hydra.merc.margin;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created By aalamer on 08-21-2019
 */
@Data
@AllArgsConstructor(staticName = "of")
public final class Counterparts {
    private final Margin longCounterpart;
    private final Margin shortCounterpart;
}
