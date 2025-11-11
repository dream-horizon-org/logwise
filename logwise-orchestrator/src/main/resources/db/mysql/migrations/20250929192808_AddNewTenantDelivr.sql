ALTER TABLE asg_details
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE grafana_user_log
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE kafka_topic_states
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE livelogs_user_log
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE service_details
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE spark_concurrency_ladder
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE spark_scale_override
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE spark_stage_history
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE spark_submit_status
MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;