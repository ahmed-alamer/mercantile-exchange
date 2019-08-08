package com.hydra.merc.exceptions;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created By aalamer on 08-08-2019
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionBody {
    private List<String> errors;

    public static ExceptionBody singleError(String error) {
        return new ExceptionBody(ImmutableList.of(error));
    }
}
