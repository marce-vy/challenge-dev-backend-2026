package com.tenpo.challenge.application.service;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import com.tenpo.challenge.application.port.out.CallHistoryCommandPort;
import java.util.Objects;

public class RecordCallHistoryService implements RecordCallHistoryUseCase {

  private final CallHistoryCommandPort commandPort;

  public RecordCallHistoryService(CallHistoryCommandPort commandPort) {
    this.commandPort = Objects.requireNonNull(commandPort, "commandPort is required");
  }

  @Override
  public void execute(RecordCallHistoryCommand command) {
    commandPort.save(Objects.requireNonNull(command, "command is required"));
  }
}
