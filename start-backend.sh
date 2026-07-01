#!/bin/bash

echo "🚀 Starting Twitter-X Backend Infrastructure Services..."

# 1. Start Docker compose dependencies
docker compose up -d postgres kafka redis minio

echo "⏳ Waiting 8 seconds for Postgres, Kafka, Redis, and MinIO to initialize..."
sleep 8

# Function to run a spring boot microservice in background
run_service() {
    local dir=$1
    local name=$2
    echo "🟢 Starting service: $name..."
    mkdir -p logs
    (cd "$dir" && MAVEN_OPTS="-Xmx256m" ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx384m") > "logs/${dir}.log" 2>&1 &
}

# 2. Start Eureka discovery server first (critical dependency)
run_service "eureka-server" "Eureka Server"
echo "⏳ Waiting 12 seconds for Eureka discovery server to initialize..."
sleep 12

# 3. Start ApiGateway
run_service "ApiGateway" "API Gateway"
echo "⏳ Waiting 6 seconds for API Gateway to boot..."
sleep 6

# 4. Start all other business microservices in parallel
run_service "auth-service" "Authentication Service"
run_service "tweet-service" "Tweet Service"
run_service "social-service" "Social Service"
run_service "media-service" "Media Service"
run_service "notification-service" "Notification Service"



echo "✅ All backend microservices have been triggered to start!"
echo "ℹ️  Run 'jobs' or check console/logs to monitor startup. Eureka: http://localhost:8761"
wait
