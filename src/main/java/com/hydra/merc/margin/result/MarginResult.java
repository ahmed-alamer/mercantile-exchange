package com.hydra.merc.margin.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Created By aalamer on 08-21-2019
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class MarginResult<T> {
    private T result;
    private MarginOpenError error;

    private Type type;

    public static <T> MarginResult<T> error(Type type, MarginOpenError marginOpenError) {
        return new MarginResult<T>().setType(type).setError(marginOpenError);
    }

    public static <T> MarginResult<T> success(Type type, T result) {
        return new MarginResult<T>().setType(type).setResult(result);
    }

    @JsonIgnore
    public boolean isSuccessful() {
        return result != null;
    }

    public enum Type {
        SETTLEMENT,
        OPEN,
        INSUFFICIENT_FUNDS;
    }

}
