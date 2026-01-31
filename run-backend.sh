#!/bin/bash
# Load environment variables from .env file and run Spring Boot app

# Load .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Environment variables loaded from .env"
else
    echo "⚠ Warning: .env file not found"
fi

# Run Spring Boot application
cd Backend/api
./mvnw spring-boot:run
