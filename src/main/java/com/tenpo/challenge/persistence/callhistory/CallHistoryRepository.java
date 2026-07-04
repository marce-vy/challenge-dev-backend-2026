package com.tenpo.challenge.persistence.callhistory;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CallHistoryRepository extends ReactiveCrudRepository<CallHistoryEntity, UUID> {}
