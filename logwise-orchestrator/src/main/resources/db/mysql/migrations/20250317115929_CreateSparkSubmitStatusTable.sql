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