CREATE DATABASE IF NOT EXISTS log_central CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional: create a custom user and grant privileges
CREATE USER IF NOT EXISTS 'myapp'@'%' IDENTIFIED BY 'myapp_pass';
GRANT ALL PRIVILEGES ON log_central.* TO 'myapp'@'%';
FLUSH PRIVILEGES;

DROP TABLE IF EXISTS spark_concurrency_ladder;
CREATE TABLE `spark_concurrency_ladder` (
                                            `concurrency` bigint unsigned NOT NULL,
                                            `cores` integer unsigned NOT NULL,
                                            `tenant` enum('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP') NOT NULL
);

DROP TABLE IF EXISTS spark_stage_history;
CREATE TABLE `spark_stage_history` (
                                       `outputBytes` bigint unsigned NOT NULL,
                                       `inputRecords` bigint unsigned NOT NULL,
                                       `submissionTime` bigint unsigned NOT NULL,
                                       `completionTime` bigint unsigned NOT NULL,
                                       `coresUsed` int unsigned NOT NULL,
                                       `status` varchar(30) NOT NULL,
                                       `tenant` enum('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP') NOT NULL,
                                       `createdAt` timestamp default CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS service_details;
CREATE TABLE `service_details` (
                                   `env` varchar(128) NOT NULL,
                                   `serviceName` varchar(50) NOT NULL,
                                   `componentName` varchar(50) NOT NULL,
                                   `retentionDays` mediumint unsigned NOT NULL,
                                   `tenant` enum('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP') NOT NULL,
                                   UNIQUE KEY (`env`, `serviceName`, `componentName`, `tenant`)
);

DROP TABLE IF EXISTS livelogs_user_log;
CREATE TABLE `livelogs_user_log` (
                                     `hostname` varchar(45) NOT NULL,
                                     `command` json NOT NULL,
                                     `tenant` enum('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP') NOT NULL,
                                     `createdAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     KEY `hostname` (`hostname`)
);

DROP TABLE IF EXISTS spark_scale_override;
CREATE TABLE `spark_scale_override` (
                                        `upscale` bool NOT NULL DEFAULT true,
                                        `downscale` bool NOT NULL DEFAULT true,
                                        `tenant` enum('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP') NOT NULL,
                                        PRIMARY KEY `tenant` (`tenant`)
);

CREATE EVENT IF NOT EXISTS delete_spark_stage_history_older_that_6_hours
ON SCHEDULE
    EVERY 6 HOUR
    STARTS CURRENT_TIMESTAMP
DO
BEGIN
DELETE FROM spark_stage_history WHERE createdAt < NOW() - INTERVAL 6 HOUR;
END;


ALTER TABLE spark_concurrency_ladder MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL;
ALTER TABLE spark_stage_history MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL;
ALTER TABLE service_details MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL;
ALTER TABLE livelogs_user_log MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL;
ALTER TABLE spark_scale_override MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL;

DROP TABLE IF EXISTS spark_submit_status;
CREATE TABLE `spark_submit_status`
(
    `id`                                int auto_increment,
    `startingOffsetsTimestamp`          bigint unsigned                                                                            NOT NULL,
    `resumeToSubscribePatternTimestamp` bigint unsigned                                                                            NOT NULL,
    `isSubmittedForOffsetsTimestamp`    boolean   default false,
    `isResumedToSubscribePattern`       boolean   default false,
    `tenant`                            enum ('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL,
    `createdAt`                         timestamp default CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `startingOffsetsTimestamp_UNIQUE` (`startingOffsetsTimestamp`)
);

ALTER TABLE livelogs_user_log ADD INDEX idx_createdAt (createdAt);

DROP TABLE IF EXISTS asg_details;
CREATE TABLE `asg_details`
(
    `accountId`            varchar(16)                                                                                NOT NULL,
    `autoScalingGroupName` varchar(256)                                                                               NOT NULL,
    `retentionDays`        mediumint unsigned                                                                         NOT NULL,
    `tenant`               enum ('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'D11-Prod-Logs-GCP', 'Hulk-Prod-AWS') NOT NULL,
    `createdAt`            timestamp default CURRENT_TIMESTAMP,
    UNIQUE KEY (`accountId`, `autoScalingGroupName`),
    INDEX idx_tenant (`tenant`),
    INDEX idx_createdAt (`createdAt`)
);

CREATE TABLE `grafana_user_log` (
                                    `id` INT NOT NULL AUTO_INCREMENT,
                                    `submissionTime` DATETIME DEFAULT NULL,
                                    `queryId` VARCHAR(100) NOT NULL,
                                    `dashBoard` VARCHAR(100) DEFAULT NULL,
                                    `userName` VARCHAR(100) DEFAULT NULL,
                                    `query` TEXT,
                                    `dataScannedInBytes` BIGINT DEFAULT 0,
                                    `engineExecutionTimeInMillis` INT DEFAULT 0,
                                    `totalExecutionTimeInMillis` INT DEFAULT 0,
                                    `queryQueueTimeInMillis` INT DEFAULT 0,
                                    `servicePreProcessingTimeInMillis` INT DEFAULT 0,
                                    `queryPlanningTimeInMillis` INT DEFAULT 0,
                                    `serviceProcessingTimeInMillis` INT DEFAULT 0,
                                    `queryStatus` ENUM('QUEUED','RUNNING','SUCCEEDED','FAILED','CANCELLED','UNKNOWN') DEFAULT NULL,
                                    `tenant` ENUM('D11-Prod-AWS','D11-Stag-AWS','DP-Logs-AWS','D11-Prod-Logs-GCP','Hulk-Prod-AWS') NOT NULL,
                                    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uniq_queryId` (`queryId`),
                                    INDEX `idx_tenant_status` (`tenant`, `queryStatus`),
                                    INDEX `idx_tenant_submissionTime` (`tenant`, `submissionTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


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

ALTER TABLE service_details ADD COLUMN lastCheckedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

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