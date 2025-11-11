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