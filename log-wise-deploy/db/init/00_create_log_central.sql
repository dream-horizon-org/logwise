-- Create DB and use it
CREATE DATABASE IF NOT EXISTS log_central
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE log_central;

-- Optional user + grant
CREATE USER IF NOT EXISTS 'myapp'@'%' IDENTIFIED BY 'myapp_pass';
GRANT ALL PRIVILEGES ON log_central.* TO 'myapp'@'%';
FLUSH PRIVILEGES;

-- Tables
DROP TABLE IF EXISTS spark_concurrency_ladder;
CREATE TABLE spark_concurrency_ladder (
  concurrency BIGINT UNSIGNED NOT NULL,
  cores INT UNSIGNED NOT NULL,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS spark_stage_history;
CREATE TABLE spark_stage_history (
  outputBytes BIGINT UNSIGNED NOT NULL,
  inputRecords BIGINT UNSIGNED NOT NULL,
  submissionTime BIGINT UNSIGNED NOT NULL,
  completionTime BIGINT UNSIGNED NOT NULL,
  coresUsed INT UNSIGNED NOT NULL,
  status VARCHAR(30) NOT NULL,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP') NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_createdAt (createdAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS service_details;
CREATE TABLE service_details (
  env VARCHAR(128) NOT NULL,
  serviceName VARCHAR(50) NOT NULL,
  componentName VARCHAR(50) NOT NULL,
  retentionDays MEDIUMINT UNSIGNED NOT NULL,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP') NOT NULL,
  lastCheckedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_service (env, serviceName, componentName, tenant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS livelogs_user_log;
CREATE TABLE livelogs_user_log (
  hostname VARCHAR(45) NOT NULL,
  command JSON NOT NULL,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP') NOT NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY hostname (hostname),
  KEY idx_createdAt (createdAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS spark_scale_override;
CREATE TABLE spark_scale_override (
  upscale BOOL NOT NULL DEFAULT TRUE,
  downscale BOOL NOT NULL DEFAULT TRUE,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP') NOT NULL,
  PRIMARY KEY (tenant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Safe event: use custom delimiter
-- (Also ensure: SET GLOBAL event_scheduler = ON;)
DELIMITER $$
CREATE EVENT IF NOT EXISTS delete_spark_stage_history_older_than_6_hours
ON SCHEDULE EVERY 6 HOUR
STARTS CURRENT_TIMESTAMP
DO
BEGIN
  DELETE FROM spark_stage_history
  WHERE createdAt < NOW() - INTERVAL 6 HOUR;
END$$
DELIMITER ;

-- Extend tenant enums to add Hulk-Prod-AWS
ALTER TABLE spark_concurrency_ladder MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL;
ALTER TABLE spark_stage_history     MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL;
ALTER TABLE service_details         MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL;
ALTER TABLE livelogs_user_log       MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL;
ALTER TABLE spark_scale_override    MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL;

DROP TABLE IF EXISTS spark_submit_status;
CREATE TABLE spark_submit_status (
  id INT AUTO_INCREMENT,
  startingOffsetsTimestamp BIGINT UNSIGNED NOT NULL,
  resumeToSubscribePatternTimestamp BIGINT UNSIGNED NOT NULL,
  isSubmittedForOffsetsTimestamp BOOLEAN DEFAULT FALSE,
  isResumedToSubscribePattern BOOLEAN DEFAULT FALSE,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY startingOffsetsTimestamp_UNIQUE (startingOffsetsTimestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS asg_details;
CREATE TABLE asg_details (
  accountId VARCHAR(16) NOT NULL,
  autoScalingGroupName VARCHAR(256) NOT NULL,
  retentionDays MEDIUMINT UNSIGNED NOT NULL,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_asg (accountId, autoScalingGroupName),
  INDEX idx_tenant (tenant),
  INDEX idx_createdAt (createdAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS grafana_user_log;
CREATE TABLE grafana_user_log (
  id INT NOT NULL AUTO_INCREMENT,
  submissionTime DATETIME DEFAULT NULL,
  queryId VARCHAR(100) NOT NULL,
  dashBoard VARCHAR(100) DEFAULT NULL,
  userName VARCHAR(100) DEFAULT NULL,
  query TEXT,
  dataScannedInBytes BIGINT DEFAULT 0,
  engineExecutionTimeInMillis INT DEFAULT 0,
  totalExecutionTimeInMillis INT DEFAULT 0,
  queryQueueTimeInMillis INT DEFAULT 0,
  servicePreProcessingTimeInMillis INT DEFAULT 0,
  queryPlanningTimeInMillis INT DEFAULT 0,
  serviceProcessingTimeInMillis INT DEFAULT 0,
  queryStatus ENUM('QUEUED','RUNNING','SUCCEEDED','FAILED','CANCELLED','UNKNOWN') DEFAULT NULL,
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uniq_queryId (queryId),
  INDEX idx_tenant_status (tenant, queryStatus),
  INDEX idx_tenant_submissionTime (tenant, submissionTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS kafka_topic_states;
CREATE TABLE kafka_topic_states (
  tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL,
  topic_name VARCHAR(249) NOT NULL,
  state ENUM('active','inactive') NOT NULL,
  first_checked_at DATETIME NOT NULL,
  last_checked_at DATETIME NOT NULL,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME NULL,
  PRIMARY KEY (tenant, topic_name),
  INDEX idx_tenant_state_deleted_checked (tenant, state, is_deleted, first_checked_at, last_checked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add Delivr-AWS to all tenant enums
ALTER TABLE asg_details           MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE grafana_user_log      MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE kafka_topic_states    MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE livelogs_user_log     MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE service_details       MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE spark_concurrency_ladder MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE spark_scale_override  MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE spark_stage_history   MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
ALTER TABLE spark_submit_status   MODIFY COLUMN tenant ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS','Delivr-AWS') NOT NULL;
