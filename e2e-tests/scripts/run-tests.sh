#!/bin/bash

# E2E Test Runner Script
# This script provides various options for running Playwright tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
BROWSER="chromium"
HEADED=false
DEBUG=false
TEST_PATH=""
WORKERS=4
RETRIES=0

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to show usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -b, --browser BROWSER    Browser to use (chromium, firefox, webkit, all) [default: chromium]"
    echo "  -h, --headed            Run tests in headed mode"
    echo "  -d, --debug             Run tests in debug mode"
    echo "  -t, --test PATH         Specific test file or directory to run"
    echo "  -w, --workers NUMBER    Number of parallel workers [default: 4]"
    echo "  -r, --retries NUMBER    Number of retries for failed tests [default: 0]"
    echo "  -s, --smoke             Run only smoke tests"
    echo "  -u, --update-snapshots  Update snapshots"
    echo "  --help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                      # Run all tests in chromium"
    echo "  $0 -b firefox -h        # Run all tests in firefox with headed mode"
    echo "  $0 -t chat -d           # Debug chat tests"
    echo "  $0 -s                   # Run smoke tests only"
    exit 1
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
        -w|--workers)
            WORKERS="$2"
            shift 2
            ;;
        -r|--retries)
            RETRIES="$2"
            shift 2
            ;;
        -s|--smoke)
            TEST_PATH="tests/smoke"
            shift
            ;;
        -u|--update-snapshots)
            UPDATE_SNAPSHOTS=true
            shift
            ;;
        --help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
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

# Check if Playwright browsers are installed
if ! npx playwright --version > /dev/null 2>&1; then
    print_color $YELLOW "Installing Playwright browsers..."
    npx playwright install
fi

# Build the command
CMD="npx playwright test"

# Add test path if specified
if [ -n "$TEST_PATH" ]; then
    CMD="$CMD $TEST_PATH"
fi

# Add browser option
if [ "$BROWSER" != "all" ]; then
    CMD="$CMD --project=$BROWSER"
fi

# Add headed option
if [ "$HEADED" = true ]; then
    CMD="$CMD --headed"
fi

# Add debug option
if [ "$DEBUG" = true ]; then
    CMD="$CMD --debug"
    WORKERS=1
fi

# Add workers option
CMD="$CMD --workers=$WORKERS"

# Add retries option
if [ $RETRIES -gt 0 ]; then
    CMD="$CMD --retries=$RETRIES"
fi

# Add update snapshots option
if [ "$UPDATE_SNAPSHOTS" = true ]; then
    CMD="$CMD --update-snapshots"
fi

# Check if Spring Boot app is running
if ! curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_color $YELLOW "Spring Boot application is not running."
    echo "Do you want to start it? (y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        print_color $GREEN "Starting Spring Boot application..."
        cd ..
        ./gradlew bootRun &
        SPRING_PID=$!
        cd e2e-tests
        
        # Wait for app to start
        print_color $YELLOW "Waiting for application to start..."
        timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health 2>/dev/null; do sleep 2; done'
        
        if [ $? -eq 0 ]; then
            print_color $GREEN "Application started successfully!"
        else
            print_color $RED "Failed to start application!"
            exit 1
        fi
    else
        print_color $RED "Please start the Spring Boot application first."
        exit 1
    fi
fi

# Run the tests
print_color $GREEN "Running E2E tests..."
print_color $YELLOW "Command: $CMD"
echo ""

# Execute the command
$CMD

# Store the exit code
EXIT_CODE=$?

# Clean up if we started Spring Boot
if [ -n "$SPRING_PID" ]; then
    print_color $YELLOW "Stopping Spring Boot application..."
    kill $SPRING_PID 2>/dev/null || true
fi

# Show results
if [ $EXIT_CODE -eq 0 ]; then
    print_color $GREEN "✓ All tests passed!"
else
    print_color $RED "✗ Some tests failed!"
    echo ""
    echo "To view the test report, run:"
    echo "  npm run test:report"
fi

exit $EXIT_CODE