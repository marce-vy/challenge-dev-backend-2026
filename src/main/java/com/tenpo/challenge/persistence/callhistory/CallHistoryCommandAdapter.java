package com.tenpo.challenge.persistence.callhistory;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.out.CallHistoryPersistencePort;
import java.util.Objects;
import reactor.core.publisher.Mono;

public class CallHistoryCommandAdapter implements CallHistoryPersistencePort {

  private final CallHistoryRepository repository;

  public CallHistoryCommandAdapter(CallHistoryRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository is required");
  }

  @Override
  public Mono<Void> save(RecordCallHistoryCommand command) {
    return repository.save(CallHistoryPersistenceMapper.toEntity(command)).then();
  }
}
