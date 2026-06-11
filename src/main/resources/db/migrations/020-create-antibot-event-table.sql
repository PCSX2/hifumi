CREATE TABLE IF NOT EXISTS
  "antibot_event" (
    "id"    INTEGER NOT NULL UNIQUE,
    "timestamp" INTEGER NOT NULL,
    "fk_user"   INTEGER NOT NULL,
    PRIMARY KEY("id" AUTOINCREMENT)
);