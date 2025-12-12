#!/bin/bash

# Multiplayer Chess API - curl Examples
# Make this file executable: chmod +x CURL_EXAMPLES.sh

BASE_URL="http://localhost:8080"
TOKEN=""

echo "🎮 Multiplayer Chess API Testing"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 1. Register new user
echo -e "${BLUE}1. Registering new user...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "Password1!",
    "displayName": "Test User"
  }')

echo "$REGISTER_RESPONSE" | jq '.'
echo ""

# 2. Login as Alice
echo -e "${BLUE}2. Logging in as Alice...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Password1!"
  }')

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')
echo "Token: $TOKEN"
echo "$LOGIN_RESPONSE" | jq '.'
echo ""

# 3. Get online players
echo -e "${BLUE}3. Getting online players...${NC}"
curl -s -X GET "$BASE_URL/api/lobby/players" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# 4. Get active game
echo -e "${BLUE}4. Checking for active game...${NC}"
curl -s -X GET "$BASE_URL/api/game/active" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# 5. Get game by ID (assuming game 1 exists)
echo -e "${BLUE}5. Getting game details (ID: 1)...${NC}"
curl -s -X GET "$BASE_URL/api/game/1" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# 6. Get game moves
echo -e "${BLUE}6. Getting moves for game 1...${NC}"
curl -s -X GET "$BASE_URL/api/game/1/moves" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# 7. Get user's game history
echo -e "${BLUE}7. Getting user's game history...${NC}"
curl -s -X GET "$BASE_URL/api/game/history" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

echo -e "${GREEN}✅ All API tests completed!${NC}"
echo ""
echo "Note: WebSocket operations (moves, invitations) require a WebSocket client."
echo "Use the frontend application or a tool like wscat for WebSocket testing."
echo ""
echo "WebSocket URL: ws://localhost:8080/ws?token=$TOKEN"



