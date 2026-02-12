package com.flowpilot.transfer.service;

import com.flowpilot.transfer.domain.TransferJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferJobRepository extends JpaRepository<TransferJob, UUID> {
}
