package com.mossman.domain.entities;

import java.util.UUID;

public record TimeLog(
        long id,
        long ticketId,
        UUID workerId,
        String workerName,
        long minutes,
        String note,
        long loggedAt
) {}
