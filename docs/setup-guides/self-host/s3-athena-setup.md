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

## 3) Create AWS Glue Database and Table

### Create Database

1. Go to **AWS Glue Service** → **Data Catalog** → **Databases**
2. Click **Create database**
3. Set the database name to: `logs`
4. Set the location to the S3 URI you copied in step 2 (e.g., `s3://your-bucket-name/logs/`)
5. Click **Create database**

### Create Table

1. In **AWS Glue** → **Data Catalog** → **Tables**, click **Create table**
2. Configure the table:
   - **Name**: `application-logs`
   - **Database**: Select the `logs` database you just created
   - **Table format**: AWS Glue Table
   - **Data store**: S3
   - **Location**: Paste the copied S3 URI path (e.g., `s3://your-bucket-name/logs/`)
   - **Data format**: Parquet
3. Click **Next**
4. Choose **Define or Upload Schema**, then select **Edit schema as JSON**
5. Paste the following schema:

```json
[
  {
    "Name": "ddtags",
    "Type": "string"
  },
  {
    "Name": "hostname",
    "Type": "string"
  },
  {
    "Name": "message",
    "Type": "string"
  },
  {
    "Name": "source_type",
    "Type": "string"
  },
  {
    "Name": "status",
    "Type": "string"
  },
  {
    "Name": "timestamp",
    "Type": "string"
  },
  {
    "Name": "env",
    "Type": "string",
    "PartitionKey": "Partition (0)"
  },
  {
    "Name": "service_name",
    "Type": "string",
    "PartitionKey": "Partition (1)"
  },
  {
    "Name": "component_name",
    "Type": "string",
    "PartitionKey": "Partition (2)"
  },
  {
    "Name": "time",
    "Type": "string",
    "PartitionKey": "Partition (3)"
  }
]
```

6. Click **Next** and then **Create table**

::: warning Important
The table schema includes partition keys (`env`, `service_name`, `component_name`, `time`) which are essential for efficient querying in Athena. Ensure your log data is organized in S3 with these partitions in the path structure.
:::

## 4) Create Athena Workgroup

1. Go to **Amazon Athena** → **Workgroups**
2. Click **Create workgroup**
3. Configure the workgroup:
   - Set the output location to your S3 bucket's `athena-output` folder (e.g., `s3://your-bucket-name/athena-output/`)
4. Complete the workgroup creation

::: tip
The workgroup's output location stores query results. Make sure you have appropriate IAM permissions for Athena to write to this location.
:::
