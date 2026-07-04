package com.tenpo.challenge.api.callhistory;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated call-history response.")
public record CallHistoryPageResponse(
    @Schema(description = "History records for the requested page.")
        List<CallHistoryResponse> content,
    @Schema(description = "Zero-based page index.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Total number of records available.", example = "42") long totalElements,
    @Schema(description = "Total number of pages available.", example = "3") int totalPages,
    @Schema(description = "Whether another page exists after this one.", example = "true")
        boolean hasNext,
    @Schema(description = "Whether a page exists before this one.", example = "false")
        boolean hasPrevious) {

  public CallHistoryPageResponse {
    content = List.copyOf(content);
  }
}
