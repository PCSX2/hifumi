CREATE TABLE IF NOT EXISTS
  "spamkick_event" (
    "id"    INTEGER NOT NULL UNIQUE,
    "timestamp" INTEGER NOT NULL,
    "fk_user"   INTEGER NOT NULL,
    "type"  TEXT NOT NULL,
    "fk_message"    INTEGER,
    PRIMARY KEY("id" AUTOINCREMENT)
);