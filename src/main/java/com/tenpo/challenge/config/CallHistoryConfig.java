package com.tenpo.challenge.config;

import com.tenpo.challenge.api.callhistory.CallHistoryFilter;
import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import com.tenpo.challenge.application.port.out.CallHistoryCommandPort;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import com.tenpo.challenge.application.port.out.CallHistoryRecorder;
import com.tenpo.challenge.application.service.GetCallHistoryService;
import com.tenpo.challenge.application.service.RecordCallHistoryService;
import com.tenpo.challenge.infrastructure.callhistory.AsyncCallHistoryRecorder;
import com.tenpo.challenge.persistence.callhistory.CallHistoryCommandAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryQueryAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class CallHistoryConfig {

  @Bean
  public CallHistoryQueryPort callHistoryQueryPort(CallHistoryRepository repository) {
    return new CallHistoryQueryAdapter(repository);
  }

  @Bean
  public GetCallHistoryUseCase getCallHistoryUseCase(CallHistoryQueryPort queryPort) {
    return new GetCallHistoryService(queryPort);
  }

  @Bean(name = "callHistoryExecutor")
  public SimpleAsyncTaskExecutor callHistoryExecutor(CallHistoryAsyncProperties properties) {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("call-history-");
    executor.setVirtualThreads(true);
    executor.setConcurrencyLimit(properties.concurrencyLimit());
    return executor;
  }

  @Bean
  public CallHistoryCommandPort callHistoryCommandPort(CallHistoryRepository repository) {
    return new CallHistoryCommandAdapter(repository);
  }

  @Bean
  public RecordCallHistoryUseCase recordCallHistoryUseCase(CallHistoryCommandPort commandPort) {
    return new RecordCallHistoryService(commandPort);
  }

  @Bean
  public CallHistoryRecorder callHistoryRecorder(
      RecordCallHistoryUseCase recordCallHistoryUseCase) {
    return new AsyncCallHistoryRecorder(recordCallHistoryUseCase);
  }

  @Bean
  public CallHistoryFilter callHistoryFilter(
      CallHistoryRecorder recorder, ClientIpResolver clientIpResolver) {
    return new CallHistoryFilter(recorder, clientIpResolver);
  }
}
