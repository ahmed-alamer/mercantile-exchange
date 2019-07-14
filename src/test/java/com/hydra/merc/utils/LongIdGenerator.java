package com.hydra.merc.utils;

import lombok.NoArgsConstructor;

/**
 * Created By ahmed on 07-14-2019
 */
@NoArgsConstructor(staticName = "create")
public class LongIdGenerator implements IdGenerator<Long> {
    private long nextValue = 1;

    @Override
    public Long generate() {
        return nextValue++;
    }
}
