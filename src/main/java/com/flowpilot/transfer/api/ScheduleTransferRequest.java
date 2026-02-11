package com.flowpilot.transfer.api;

import java.time.Instant;

public record ScheduleTransferRequest(Instant scheduledAt) {
}
