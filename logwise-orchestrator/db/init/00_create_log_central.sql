CREATE DATABASE IF NOT EXISTS log_central CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional: create a custom user and grant privileges
CREATE USER IF NOT EXISTS 'myapp'@'%' IDENTIFIED BY 'myapp_pass';
GRANT ALL PRIVILEGES ON log_central.* TO 'myapp'@'%';
FLUSH PRIVILEGES;

DROP TABLE IF EXISTS service_details;
CREATE TABLE `service_details` (
                                   `env` varchar(128) NOT NULL,
                                   `serviceName` varchar(50) NOT NULL,
                                   `componentName` varchar(50) NOT NULL,
                                   `retentionDays` mediumint unsigned NOT NULL,
                                   `tenant` enum('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS') NOT NULL,
                                   UNIQUE KEY (`env`, `serviceName`, `componentName`, `tenant`)
);

DROP TABLE IF EXISTS spark_submit_status;
CREATE TABLE `spark_submit_status`
(
    `id`                                int auto_increment,
    `startingOffsetsTimestamp`          bigint unsigned                                                                            NOT NULL,
    `resumeToSubscribePatternTimestamp` bigint unsigned                                                                            NOT NULL,
    `isSubmittedForOffsetsTimestamp`    boolean   default false,
    `isResumedToSubscribePattern`       boolean   default false,
    `tenant`                            enum ('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'Hulk-Prod-AWS') NOT NULL,
    `createdAt`                         timestamp default CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `startingOffsetsTimestamp_UNIQUE` (`startingOffsetsTimestamp`)
);

-- Add lastCheckedAt column to service_details
ALTER TABLE service_details ADD COLUMN lastCheckedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update tenant enum values for active tables
ALTER TABLE service_details
    MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;

ALTER TABLE spark_submit_status
    MODIFY COLUMN tenant ENUM('D11-Prod-AWS', 'D11-Stag-AWS', 'DP-Logs-AWS', 'Hulk-Prod-AWS', 'Delivr-AWS') NOT NULL;