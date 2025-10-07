#!/bin/bash

# Confluence Sync Task Runner
# Usage: ./run-confluence-sync.sh <spaceKey>

SPACE_KEY=${1:-XXXXX}

echo "=================================================="
echo "Starting Confluence Sync for space: $SPACE_KEY"
echo "=================================================="
echo ""

# Run the sync task with proper JVM options
./gradlew bootRun \
  --args="--task.confluence-sync.enabled=true spaceKey=$SPACE_KEY" \
  2>&1 | tee logs/confluence-sync-$(date +%Y%m%d-%H%M%S).log

echo ""
echo "=================================================="
echo "Sync completed. Check logs above for details."
echo "=================================================="