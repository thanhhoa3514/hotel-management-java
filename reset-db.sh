#!/bin/bash

# Script to reset database - USE WITH CAUTION!
# This will delete ALL data

echo "WARNING: This will delete all data in the database!"
echo "Press Ctrl+C to cancel, or Enter to continue..."
read

echo "Stopping containers..."
docker-compose down

echo "Removing volumes (this deletes all data)..."
docker volume rm quanlikhachsan_postgres_data 2>/dev/null || true
docker volume rm quanlikhachsan_redis_data 2>/dev/null || true

echo "Starting containers..."
docker-compose up -d

echo "Waiting for PostgreSQL to be ready..."
sleep 10

echo "Database reset complete!"
echo "You can now run your Spring Boot application."
echo ""
echo "Check logs with: docker-compose logs -f"

