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
  `environmentName` varchar(128) NOT NULL,
  `componentType` varchar(50) NOT NULL,
  `serviceName` varchar(50) NOT NULL,
  `retentionDays` mediumint unsigned NOT NULL,
  `tenant` enum('ABC') NOT NULL,
  `lastCheckedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY (`environmentName`, `componentType`, `serviceName`, `tenant`)
);
