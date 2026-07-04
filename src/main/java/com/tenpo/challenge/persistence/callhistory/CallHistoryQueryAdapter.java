package com.tenpo.challenge.persistence.callhistory;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public class CallHistoryQueryAdapter implements CallHistoryQueryPort {

  private final CallHistoryRepository repository;

  public CallHistoryQueryAdapter(CallHistoryRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  @Override
  @Transactional(readOnly = true)
  public CallHistoryPage findPage(PaginationRequest request) {
    Objects.requireNonNull(request, "request is required");
    PageRequest pageRequest =
        PageRequest.of(
            request.page(),
            request.size(),
            Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
    Page<CallHistoryEntity> page = repository.findAll(pageRequest);
    List<CallHistoryEntry> content =
        page.getContent().stream().map(CallHistoryPersistenceMapper::toEntry).toList();

    return new CallHistoryPage(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext(),
        page.hasPrevious());
  }
}
