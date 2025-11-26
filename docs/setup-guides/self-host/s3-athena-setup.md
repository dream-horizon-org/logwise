---
title: S3 & Athena (Self-Host)
---

# S3 & Athena — Self-Hosted Setup

Follow these steps to set up Amazon S3 and Athena for LogWise to store and query your application logs.

## Prerequisites

- An AWS account with appropriate permissions to create S3 buckets, Glue databases/tables, and Athena workgroups
- Access to AWS Console (or AWS CLI configured)

## 1) Create S3 Bucket and Folders

1. Go to **Amazon S3** in the AWS Console
2. Create a new S3 bucket (or use an existing one)
3. Inside the bucket, create two folders:
   - `logs` — for storing log data
   - `athena-output` — for Athena query results

## 2) Copy S3 URI of Logs Directory

1. Navigate to the `logs` folder you just created
2. Copy the S3 URI (e.g., `s3://your-bucket-name/logs/`)
3. You'll need this URI in the next steps

## 3) Create AWS Glue Database

1. Go to **AWS Glue Service** → **Data Catalog** → **Databases**
2. Click **Create database**
3. Set the database name to: `logs`
4. Set the location to the S3 URI you copied in step 2 (e.g., `s3://your-bucket-name/logs/`)
5. Click **Create database**

## 4) Create Athena Workgroup

1. Go to **Amazon Athena** → **Workgroups**
2. Click **Create workgroup**
3. Configure the workgroup:
   - Set the output location to your S3 bucket's `athena-output` folder (e.g., `s3://your-bucket-name/athena-output/`)
4. Complete the workgroup creation

::: tip
The workgroup's output location stores query results. Make sure you have appropriate IAM permissions for Athena to write to this location.
:::

## 5) Create Table

1. Go to **Amazon Athena** → **Query editor**
2. In the query editor, select the workgroup you created in step 4 from the workgroup dropdown
3. Run the following DDL to create the table:

```sql
CREATE EXTERNAL TABLE `application-logs`(
  `hostname` string, 
  `message` string, 
  `source_type` string, 
  `timestamp` string)
PARTITIONED BY (   
  `service_name` string, 
  `time` string)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
LOCATION
  's3://your-bucket-name/logs'
TBLPROPERTIES (
  'compressionType'='gzip', 
  'parquet.ignore.statistics'='true', 
  'projection.enabled'='true', 
  'projection.service_name.type'='injected', 
  'projection.time.format'='\'year=\'yyyy\'/month=\'MM\'/day=\'dd\'/hour=\'HH\'/minute=\'mm', 
  'projection.time.interval'='1', 
  'projection.time.interval.unit'='MINUTES', 
  'projection.time.range'='NOW-1YEARS,NOW', 
  'projection.time.type'='date', 
  'storage.location.template'='s3://your-bucket-name/logs/service_name=${service_name}/${time}')
```

::: warning Important
The table schema includes partition keys (`service_name`, `time`) which are essential for efficient querying in Athena. Ensure your log data is organized in S3 with these partitions in the path structure.

**Before running the query**, replace `s3://your-bucket-name/logs` with your actual S3 URI from step 2 in both the `LOCATION` clause and the `storage.location.template` property.
:::
