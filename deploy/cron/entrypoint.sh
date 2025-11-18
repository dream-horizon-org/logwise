#!/bin/sh
set -e

# Install dependencies
apk add --no-cache curl tzdata

# Try multiple possible locations for crontab file
CRONTAB_FILE=""
for path in /app/cron/crontab /crontab /etc/cron.d/crontab /app/crontab /tmp/crontab; do
    if [ -f "$path" ]; then
        CRONTAB_FILE="$path"
        break
    fi
done

# If not found as file, check if mounted as directory
if [ -z "$CRONTAB_FILE" ]; then
    if [ -d /app/cron ] && [ -f /app/cron/crontab ]; then
        CRONTAB_FILE="/app/cron/crontab"
    elif [ -d /crontab ] && [ -f /crontab/crontab ]; then
        CRONTAB_FILE="/crontab/crontab"
    elif [ -d /etc/cron.d ] && [ -f /etc/cron.d/crontab ]; then
        CRONTAB_FILE="/etc/cron.d/crontab"
    fi
fi

# Install crontab
if [ -n "$CRONTAB_FILE" ] && [ -f "$CRONTAB_FILE" ]; then
    echo "Installing crontab from $CRONTAB_FILE"
    crontab - < "$CRONTAB_FILE"
else
    echo "Error: Could not find crontab file. Checked: /app/cron/crontab, /crontab, /etc/cron.d/crontab, /app/crontab, /tmp/crontab" >&2
    echo "Contents of /app/cron:" >&2
    ls -la /app/cron >&2 || true
    echo "Contents of /crontab:" >&2
    ls -la /crontab >&2 || true
    exit 1
fi

# Start crond
exec crond -f -l 8
