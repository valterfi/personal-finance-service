#!/bin/bash
set -euo pipefail

REMOTE_PROJECT_PATH="/home/ubuntu/personal-finance/personal-finance-service"
VOLUME_NAME="personal-finance_postgres-data"

PEM_KEY=""
SERVER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -key)
      PEM_KEY="$2"
      shift 2
      ;;
    -server)
      SERVER="$2"
      shift 2
      ;;
    -remote-path)
      REMOTE_PROJECT_PATH="$2"
      shift 2
      ;;
    -volume)
      VOLUME_NAME="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage:"
      echo "  $0 -key <pem-file> -server <server>"
      echo ""
      echo "Optional:"
      echo "  -remote-path <path>"
      echo "  -volume <volume-name>"
      exit 0
      ;;
    *)
      echo "Unknown parameter: $1"
      exit 1
      ;;
  esac
done

if [[ -z "$PEM_KEY" ]]; then
  echo "Missing required parameter: -key"
  exit 1
fi

if [[ -z "$SERVER" ]]; then
  echo "Missing required parameter: -server"
  exit 1
fi

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="postgres-data-$TIMESTAMP.tar.gz"

LOCAL_BACKUP_PATH="/tmp/$BACKUP_FILE"
REMOTE_BACKUP_PATH="/tmp/$BACKUP_FILE"

echo "================================================="
echo "Remote Server : $SERVER"
echo "Remote Project: $REMOTE_PROJECT_PATH"
echo "Volume        : $VOLUME_NAME"
echo "Backup File   : $BACKUP_FILE"
echo "PEM Key       : $PEM_KEY"
echo "================================================="

echo ""
echo "Stopping remote docker compose..."
ssh -i "$PEM_KEY" "ubuntu@$SERVER" "
  cd '$REMOTE_PROJECT_PATH' &&
  docker compose down
"

echo ""
echo "Creating remote volume backup..."
ssh -i "$PEM_KEY" "ubuntu@$SERVER" "
  docker run --rm \
    -v '$VOLUME_NAME':/volume \
    -v /tmp:/backup \
    alpine \
    tar czf /backup/$BACKUP_FILE -C /volume . &&

  sudo chown ubuntu:ubuntu '$REMOTE_BACKUP_PATH'
"

echo ""
echo "Starting remote docker compose..."
ssh -i "$PEM_KEY" "ubuntu@$SERVER" "
  cd '$REMOTE_PROJECT_PATH' &&
  docker compose up -d
"

echo ""
echo "Copying backup to local machine..."
scp -i "$PEM_KEY" \
  "ubuntu@$SERVER:$REMOTE_BACKUP_PATH" \
  "$LOCAL_BACKUP_PATH"

echo ""
echo "Stopping local docker compose..."
docker compose down

echo ""
echo "Removing local volume (if exists)..."
docker volume rm "$VOLUME_NAME" 2>/dev/null || true

echo ""
echo "Creating local volume..."
docker volume create "$VOLUME_NAME"

echo ""
echo "Restoring backup into local volume..."
docker run --rm \
  -v "$VOLUME_NAME":/volume \
  -v /tmp:/backup \
  alpine \
  sh -c "tar xzf /backup/$BACKUP_FILE -C /volume"

echo ""
echo "================================================="
echo "Migration completed successfully!"
echo "Remote server restarted."
echo "Remote backup file: $REMOTE_BACKUP_PATH"
echo "Local backup file : $LOCAL_BACKUP_PATH"
echo "Volume restored   : $VOLUME_NAME"
echo "================================================="