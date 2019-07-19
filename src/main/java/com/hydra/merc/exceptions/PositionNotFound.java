package com.hydra.merc.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created By aalamer on 07-19-2019
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Position was not found")
public class PositionNotFound extends Exception {
    public PositionNotFound(long positionId) {
        super(String.format("Position %d was not found", positionId));
    }
}
