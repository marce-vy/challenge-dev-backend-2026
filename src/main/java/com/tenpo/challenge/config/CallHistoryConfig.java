package com.tenpo.challenge.config;

import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import com.tenpo.challenge.application.port.out.CallHistoryPersistencePort;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import com.tenpo.challenge.application.port.out.ClientIpResolver;
import com.tenpo.challenge.application.service.GetCallHistoryService;
import com.tenpo.challenge.application.service.RecordCallHistoryService;
import com.tenpo.challenge.infrastructure.callhistory.AsyncCallHistoryRecorder;
import com.tenpo.challenge.infrastructure.callhistory.CallHistoryRecorder;
import com.tenpo.challenge.infrastructure.callhistory.CallHistoryWebFilter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryCommandAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryQueryAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class CallHistoryConfig {

  @Bean
  public CallHistoryQueryPort callHistoryQueryPort(R2dbcEntityTemplate template) {
    return new CallHistoryQueryAdapter(template);
  }

  @Bean
  public GetCallHistoryUseCase getCallHistoryUseCase(CallHistoryQueryPort queryPort) {
    return new GetCallHistoryService(queryPort);
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
  public CallHistoryWebFilter callHistoryWebFilter(
      CallHistoryRecorder recorder, ClientIpResolver clientIpResolver) {
    return new CallHistoryWebFilter(recorder, clientIpResolver);
  }
}
