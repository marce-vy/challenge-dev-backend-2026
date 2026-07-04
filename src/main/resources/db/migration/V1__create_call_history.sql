CREATE TABLE call_history (
  id UUID PRIMARY KEY,
  occurred_at TIMESTAMPTZ NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  query_params TEXT,
  request_body TEXT,
  response_body TEXT,
  error_body TEXT,
  http_status INTEGER NOT NULL,
  success BOOLEAN NOT NULL,
  duration_ms BIGINT,
  client_ip VARCHAR(64)
);

CREATE INDEX idx_call_history_occurred_at
  ON call_history (occurred_at DESC, id DESC);
