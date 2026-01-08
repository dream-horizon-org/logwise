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
DROP TABLE IF EXISTS service_details;
    
CREATE TABLE `service_details` (
  `serviceName` varchar(50) NOT NULL,
  `retentionDays` mediumint unsigned NOT NULL,
  `tenant` enum('ABC') NOT NULL,
  `lastCheckedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY (`serviceName`, `tenant`)
);


DROP TABLE IF EXISTS spark_scale_override;
CREATE TABLE `spark_scale_override` (
  `upscale` bool NOT NULL DEFAULT true,
  `downscale` bool NOT NULL DEFAULT true,
  `tenant` enum('ABC') NOT NULL,
   PRIMARY KEY `tenant` (`tenant`)
);


DROP TABLE IF EXISTS spark_stage_history;
CREATE TABLE `spark_stage_history` (
  `outputBytes` bigint unsigned NOT NULL,
  `inputRecords` bigint unsigned NOT NULL,
  `submissionTime` bigint unsigned NOT NULL,
  `completionTime` bigint unsigned NOT NULL,
  `coresUsed` int unsigned NOT NULL,
  `status` varchar(30) NOT NULL,
  `tenant` enum('ABC') NOT NULL,
  `createdAt` timestamp default CURRENT_TIMESTAMP
);
