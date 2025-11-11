CREATE TABLE kafka_topic_states (
    tenant enum ('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL,
    topic_name VARCHAR(249) NOT NULL,
    state ENUM('active', 'inactive') NOT NULL,
    first_checked_at DATETIME NOT NULL,
    last_checked_at DATETIME NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (tenant, topic_name),

    INDEX idx_tenant_state_deleted_checked (
        tenant, state, is_deleted, first_checked_at, last_checked_at)
);

ALTER TABLE kafka_topic_states
ADD COLUMN deleted_at DATETIME NULL AFTER is_deleted;