package com.tenpo.challenge.persistence.callhistory;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CallHistoryQueryAdapter implements CallHistoryQueryPort {

  private final R2dbcEntityTemplate template;

  public CallHistoryQueryAdapter(R2dbcEntityTemplate template) {
    this.template = Objects.requireNonNull(template);
  }

  @Override
  public Mono<CallHistoryPage> findPage(PaginationRequest request) {
    Objects.requireNonNull(request, "request is required");
    PageRequest pageable = PageRequest.of(request.page(), request.size());

    Query query =
        Query.empty()
            .sort(Sort.by(Sort.Order.desc("occurred_at"), Sort.Order.desc("id")))
            .with(pageable);

    Mono<Long> countMono =
        template
            .count(Query.empty(), CallHistoryEntity.class)
            .subscribeOn(Schedulers.boundedElastic());
    Mono<java.util.List<CallHistoryEntity>> listMono =
        template
            .select(CallHistoryEntity.class)
            .matching(query)
            .all()
            .collectList()
            .subscribeOn(Schedulers.boundedElastic());

    return Mono.zip(countMono, listMono)
        .map(
            tuple -> {
              long total = tuple.getT1();
              java.util.List<CallHistoryEntry> content =
                  tuple.getT2().stream().map(CallHistoryPersistenceMapper::toEntry).toList();
              int totalPages =
                  (int) (request.size() > 0 ? (total + request.size() - 1) / request.size() : 0);
              return new CallHistoryPage(
                  content,
                  request.page(),
                  request.size(),
                  total,
                  totalPages,
                  request.page() + 1 < totalPages,
                  request.page() > 0);
            });
  }
}
