package com.tenpo.challenge.config;

import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import com.tenpo.challenge.application.port.out.CallHistoryPersistencePort;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import com.tenpo.challenge.application.service.GetCallHistoryService;
import com.tenpo.challenge.application.service.RecordCallHistoryService;
import com.tenpo.challenge.infrastructure.callhistory.AsyncCallHistoryRecorder;
import com.tenpo.challenge.infrastructure.callhistory.CallHistoryFilter;
import com.tenpo.challenge.infrastructure.callhistory.CallHistoryRecorder;
import com.tenpo.challenge.persistence.callhistory.CallHistoryRepository;
import com.tenpo.challenge.persistence.callhistory.CallHistoryCommandAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryQueryAdapter;
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
  public CallHistoryPersistencePort callHistoryPersistencePort(CallHistoryRepository repository) {
    return new CallHistoryCommandAdapter(repository);
  }

  @Bean
  public RecordCallHistoryUseCase recordCallHistoryUseCase(
      CallHistoryPersistencePort persistencePort) {
    return new RecordCallHistoryService(persistencePort);
  }

  @Bean
  public AsyncCallHistoryRecorder asyncCallHistoryRecorder(
      RecordCallHistoryUseCase recordCallHistoryUseCase) {
    return new AsyncCallHistoryRecorder(recordCallHistoryUseCase);
  }

  @Bean
  public CallHistoryFilter callHistoryFilter(CallHistoryRecorder recorder, ClientIpResolver clientIpResolver) {
    return new CallHistoryFilter(recorder, clientIpResolver);
  }
}
