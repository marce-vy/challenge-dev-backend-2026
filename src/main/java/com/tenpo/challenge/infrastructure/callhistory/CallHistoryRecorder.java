package com.tenpo.challenge.infrastructure.callhistory;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;

public interface CallHistoryRecorder {

  void record(RecordCallHistoryCommand command);
}
