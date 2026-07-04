package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;

public interface RecordCallHistoryUseCase {

  void execute(RecordCallHistoryCommand command);
}
