CREATE TABLE IF NOT EXISTS
  "honeypot_event" (
    "id"    INTEGER NOT NULL UNIQUE,
    "timestamp" INTEGER NOT NULL,
    "fk_user"   INTEGER NOT NULL,
    "fk_message"    INTEGER NOT NULL,
    PRIMARY KEY("id" AUTOINCREMENT)
);