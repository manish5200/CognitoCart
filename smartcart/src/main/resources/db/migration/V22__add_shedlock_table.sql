-- ShedLock requires this exact table structure to keep track of which server holds the lock.
CREATE TABLE shedlock (
                          name       VARCHAR(64)  PRIMARY KEY,
                          lock_until TIMESTAMP    NOT NULL,
                          locked_at  TIMESTAMP    NOT NULL,
                          locked_by  VARCHAR(255) NOT NULL
);
