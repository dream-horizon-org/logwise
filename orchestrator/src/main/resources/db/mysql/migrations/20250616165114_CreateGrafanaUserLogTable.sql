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
