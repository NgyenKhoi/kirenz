#!/bin/bash
# Bash script to load environment variables from .env file
# Usage: source ./scripts/load-env.sh

if [ -f ".env" ]; then
    echo "Loading environment variables from .env file..."
    
    # Export variables from .env file
    export $(grep -v '^#' .env | xargs)
    
    echo "Environment variables loaded successfully!"
    echo "You can now run: ./mvnw spring-boot:run"
else
    echo "Error: .env file not found!"
    echo "Please copy .env.example to .env and update the values"
fi