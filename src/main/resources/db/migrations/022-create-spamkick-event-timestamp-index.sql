CREATE INDEX IF NOT EXISTS
  "idx_spamkick_event_timestamp" ON "spamkick_event" (
    "timestamp" DESC
);