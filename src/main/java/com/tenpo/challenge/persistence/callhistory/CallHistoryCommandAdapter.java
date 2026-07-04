package com.tenpo.challenge.persistence.callhistory;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.out.CallHistoryCommandPort;
import java.util.Objects;

public class CallHistoryCommandAdapter implements CallHistoryCommandPort {

  private final CallHistoryRepository repository;

  public CallHistoryCommandAdapter(CallHistoryRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository is required");
  }

  @Override
  public void save(RecordCallHistoryCommand command) {
    repository.saveAndFlush(CallHistoryPersistenceMapper.toEntity(command));
  }
}
