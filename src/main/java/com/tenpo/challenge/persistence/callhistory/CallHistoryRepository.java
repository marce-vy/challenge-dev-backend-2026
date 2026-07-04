package com.tenpo.challenge.persistence.callhistory;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallHistoryRepository extends JpaRepository<CallHistoryEntity, UUID> {}
