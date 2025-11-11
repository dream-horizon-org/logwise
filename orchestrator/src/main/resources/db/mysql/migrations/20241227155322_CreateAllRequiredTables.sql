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