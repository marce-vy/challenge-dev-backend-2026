package com.tenpo.challenge.application.service;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import com.tenpo.challenge.application.port.out.CallHistoryPersistencePort;
import java.util.Objects;
import reactor.core.publisher.Mono;

public class RecordCallHistoryService implements RecordCallHistoryUseCase {

  private final CallHistoryPersistencePort persistencePort;

  public RecordCallHistoryService(CallHistoryPersistencePort persistencePort) {
    this.persistencePort = Objects.requireNonNull(persistencePort, "persistencePort is required");
  }

  @Override
  public Mono<Void> execute(RecordCallHistoryCommand command) {
    return persistencePort.save(Objects.requireNonNull(command, "command is required"));
  }
}
