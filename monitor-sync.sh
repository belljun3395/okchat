#!/bin/bash

# Real-time Confluence Sync Monitor
# Shows key progress indicators without overwhelming output

echo "🔄 Monitoring Confluence Sync Progress..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Find the most recent log file
LOG_FILE=$(ls -t logs/application*.log 2>/dev/null | head -1)

if [ -z "$LOG_FILE" ]; then
    echo "❌ No log file found in logs/"
    exit 1
fi

echo "📄 Monitoring: $LOG_FILE"
echo "=================================================="
echo ""

# Monitor the log file for key events
tail -f "$LOG_FILE" | grep --line-buffered -E "\
\[ConfluenceSync\].*Starting|\
\[ConfluenceSync\].*Retrieved contents|\
\[ConfluenceSync\].*Converting.*pages|\
\[ConfluenceSync\].*Processing:|\
\[Batch.*Successfully added|\
\[Batch.*Failed|\
\[ConfluenceSync\].*Sync completed|\
Processing page with empty content|\
Split into.*chunks|\
Document count mismatch|\
Failed batches|\
WARN.*VectorStore|\
ERROR"