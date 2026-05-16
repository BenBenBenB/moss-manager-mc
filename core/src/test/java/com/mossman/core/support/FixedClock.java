package com.mossman.core.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public final class FixedClock extends Clock {

    private final Instant instant;
    private final ZoneId zone;

    public FixedClock(long epochMilli) {
        this(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
    }

    public FixedClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    @Override public ZoneId getZone()              { return zone; }
    @Override public Clock  withZone(ZoneId zone)  { return new FixedClock(instant, zone); }
    @Override public Instant instant()             { return instant; }
}
