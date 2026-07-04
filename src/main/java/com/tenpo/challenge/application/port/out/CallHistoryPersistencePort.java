package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;

public interface CallHistoryPersistencePort {

  void save(RecordCallHistoryCommand command);
}
