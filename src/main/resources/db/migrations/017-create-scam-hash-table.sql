CREATE TABLE IF NOT EXISTS 
  "scam_hash" (
    "sha256"    TEXT NOT NULL UNIQUE,
    "timestamp" INTEGER NOT NULL,
    "description"   TEXT NOT NULL,
    "active"    INTEGER NOT NULL,
    PRIMARY KEY("sha256")
);