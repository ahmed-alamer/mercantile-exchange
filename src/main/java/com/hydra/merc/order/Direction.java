package com.hydra.merc.order;

/**
 * Created By aalamer on 07-16-2019
 */
public enum Direction {
    LONG {
        @Override
        public Direction getAnte() {
            return SHORT;
        }
    },
    SHORT {
        @Override
        public Direction getAnte() {
            return LONG;
        }
    };

    public abstract Direction getAnte();
}
