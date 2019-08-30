package com.hydra.merc.settlement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hydra.merc.ledger.LedgerTransaction;
import com.hydra.merc.margin.Margin;
import com.hydra.merc.margin.transactions.MarginTransaction;
import com.hydra.merc.utils.Jackson;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.io.IOException;

/**
 * Created By aalamer on 07-11-2019
 */

@Data
@Slf4j
@Entity
@Table(name = "settlements")
@NoArgsConstructor
@Accessors(chain = true)
public final class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Transient
    private Leg longLeg;

    @Transient
    private Leg shortLeg;

    @Column(columnDefinition = "TEXT")
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private String longLegRaw;

    @Column(columnDefinition = "TEXT")
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private String shortLegRaw;

    public Leg getLongLeg() {
        if (longLeg == null) {
            this.longLeg = parseLeg(longLegRaw);
        }
        return longLeg;
    }

    public Settlement setLongLeg(Leg longLeg) {
        this.longLeg = longLeg;
        try {
            this.longLegRaw = Jackson.serialize(longLeg);
            return this;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Leg getShortLeg() {
        if (shortLeg == null) {
            this.shortLeg = parseLeg(shortLegRaw);
        }
        return shortLeg;
    }

    public Settlement setShortLeg(Leg shortLeg) {
        this.shortLeg = shortLeg;
        try {
            this.shortLegRaw = Jackson.serialize(shortLeg);

            return this;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Leg parseLeg(String rawLeg) {
        if (rawLeg == null) {
            return null; // TODO: Maybe throw an exception because it's a fucked up state?
        }

        try {
            return Jackson.deserialize(rawLeg, Leg.class);
        } catch (IOException e) {
            log.error("Failed to parse {}: {}", rawLeg, e);
            return null;
        }
    }

    @Data
    @NoArgsConstructor(staticName = "create")
    @Accessors(chain = true)
    public static final class Leg {
        private Margin margin;
        private MarginTransaction marginTransaction;
        private LedgerTransaction marginCall;
    }
}
