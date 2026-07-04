package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import reactor.core.publisher.Mono;

public interface CallHistoryPersistencePort {

  Mono<Void> save(RecordCallHistoryCommand command);
}
