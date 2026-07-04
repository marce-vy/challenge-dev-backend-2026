package com.tenpo.challenge.api.callhistory;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;

public interface CallHistoryRecorder {

  void record(RecordCallHistoryCommand command);
}
