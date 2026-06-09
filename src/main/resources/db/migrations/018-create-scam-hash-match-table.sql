CREATE TABLE IF NOT EXISTS
  "scam_hash_match" (
    "id"    INTEGER NOT NULL UNIQUE,
    "timestamp" INTEGER NOT NULL,
    "fk_scam_hash"  TEXT NOT NULL,
    "fk_message"    INTEGER NOT NULL,
    PRIMARY KEY("id" AUTOINCREMENT)
);