package com.mossman.domain.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable value object that holds optional filter criteria for ticket queries.
 * Fields are empty when not specified. String fields match case-insensitively.
 * The {@code labels} filter matches tickets that have at least one of the given labels.
 */
public record TicketFilter(
        Optional<String> status,
        Optional<String> type,
        Optional<String> priority,
        Optional<String> title,
        Optional<List<String>> labels
) {
    /** Convenience factory: no filters applied. */
    public static TicketFilter empty() {
        return new TicketFilter(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    /** Parse string fields from a map; no label filter applied. */
    public static TicketFilter of(Map<String, String> map) {
        return of(map, Collections.emptyList());
    }

    /** Parse string fields from a map and include an optional label filter. */
    public static TicketFilter of(Map<String, String> map, List<String> labels) {
        return new TicketFilter(
                Optional.ofNullable(map.get("status")),
                Optional.ofNullable(map.get("type")),
                Optional.ofNullable(map.get("priority")),
                Optional.ofNullable(map.get("title")),
                labels.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(labels))
        );
    }

    /** True if the filter has at least one criterion set. */
    public boolean hasAny() {
        return status.isPresent() || type.isPresent() || priority.isPresent()
                || title.isPresent() || labels.isPresent();
    }
}
