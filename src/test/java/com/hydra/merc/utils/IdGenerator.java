package com.hydra.merc.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created By ahmed on 07-13-2019
 */
public interface IdGenerator<ID> {
    ID generate();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class DoesNotSupportAutoGeneration<ID> implements IdGenerator<ID> {
        public static <ID> DoesNotSupportAutoGeneration<ID> getInstance() {
            return new DoesNotSupportAutoGeneration<>();
        }

        @Override
        public ID generate() {
            throw new UnsupportedOperationException();
        }
    }
}
