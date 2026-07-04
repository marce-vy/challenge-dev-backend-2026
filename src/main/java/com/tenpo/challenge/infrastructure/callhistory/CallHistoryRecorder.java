package com.tenpo.challenge.infrastructure.callhistory;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import reactor.core.publisher.Mono;

public interface CallHistoryRecorder {

  Mono<Void> record(RecordCallHistoryCommand command);
}
