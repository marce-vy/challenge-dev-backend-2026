package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import reactor.core.publisher.Mono;

public interface RecordCallHistoryUseCase {

  Mono<Void> execute(RecordCallHistoryCommand command);
}
