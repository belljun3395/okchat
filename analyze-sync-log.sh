#!/bin/bash

# Confluence Sync Log Analyzer
# Usage: ./analyze-sync-log.sh <log-file>

LOG_FILE=./logs/confluence-sync-20251007-214452.log

if [ -z "$LOG_FILE" ]; then
    echo "Usage: $0 <log-file>"
    echo "Example: $0 logs/confluence-sync-20251007-211830.log"
    exit 1
fi

if [ ! -f "$LOG_FILE" ]; then
    echo "Error: Log file not found: $LOG_FILE"
    exit 1
fi

echo "=================================================="
echo "Confluence Sync Log Analysis"
echo "=================================================="
echo ""

echo "📊 Summary:"
echo "----------------------------------------"
grep -E "\[ConfluenceSync\].*Sync completed" "$LOG_FILE" | tail -1
grep -E "\[ConfluenceSync\].*Retrieved contents" "$LOG_FILE" | tail -1
grep -E "\[ConfluenceSync\].*Converted documents" "$LOG_FILE" | tail -1
grep -E "\[ConfluenceSync\].*Stored/updated documents" "$LOG_FILE" | tail -1
echo ""

echo "📈 Document Count:"
echo "----------------------------------------"
grep -E "Initial OpenSearch document count" "$LOG_FILE" | tail -1
grep -E "Final OpenSearch document count" "$LOG_FILE" | tail -1
echo ""

echo "⚠️  Empty Content Pages:"
echo "----------------------------------------"
grep -E "Processing page with empty content" "$LOG_FILE" | wc -l | xargs -I {} echo "Found {} pages with empty content"
grep -E "Processing page with empty content" "$LOG_FILE" | head -5
echo ""

echo "❌ Failed Batches:"
echo "----------------------------------------"
grep -E "Failed to add batch" "$LOG_FILE" || echo "No failed batches"
grep -E "Failed batches:" "$LOG_FILE" | tail -1
echo ""

echo "⚠️  Warnings & Errors:"
echo "----------------------------------------"
echo "Total warnings:"
grep -c "WARN" "$LOG_FILE"
echo "Total errors:"
grep -c "ERROR" "$LOG_FILE"
echo ""
echo "Recent errors (last 5):"
grep "ERROR" "$LOG_FILE" | tail -5
echo ""

echo "✅ Chunked Documents:"
echo "----------------------------------------"
grep -E "Split into .* chunks" "$LOG_FILE" | wc -l | xargs -I {} echo "{} documents were split into chunks"
echo ""

echo "🔍 Keyword Extraction Failures:"
echo "----------------------------------------"
grep -c "Keyword extraction failed" "$LOG_FILE" | xargs -I {} echo "{} keyword extraction failures"
echo ""

echo "📝 Batch Processing:"
echo "----------------------------------------"
grep -E "\[Batch.*Successfully added documents" "$LOG_FILE" | tail -3
echo ""

echo "=================================================="
echo "Analysis Complete"
echo "=================================================="