#!/bin/bash

# E2E Test Runner Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Default values
BROWSER="chromium"
HEADED=false
DEBUG=false
TEST_PATH=""

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--browser)
            BROWSER="$2"
            shift 2
            ;;
        -h|--headed)
            HEADED=true
            shift
            ;;
        -d|--debug)
            DEBUG=true
            shift
            ;;
        -t|--test)
            TEST_PATH="$2"
            shift 2
            ;;
        -s|--smoke)
            TEST_PATH="tests/smoke"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Change to e2e-tests directory
cd "$(dirname "$0")/.."

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    print_color $YELLOW "Installing dependencies..."
    npm install
fi

# Build the command
CMD="npx playwright test"

if [ -n "$TEST_PATH" ]; then
    CMD="$CMD $TEST_PATH"
fi

if [ "$BROWSER" != "all" ]; then
    CMD="$CMD --project=$BROWSER"
fi

if [ "$HEADED" = true ]; then
    CMD="$CMD --headed"
fi

if [ "$DEBUG" = true ]; then
    CMD="$CMD --debug"
fi

# Run the tests
print_color $GREEN "Running E2E tests..."
$CMD
