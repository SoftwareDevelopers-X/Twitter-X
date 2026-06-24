# chat-service

Real-time 1:1 + group chat microservice for TwitterX, using STOMP over WebSocket,
routed through Spring Cloud Gateway, with PostgreSQL for persistence.

## How identity works (important)

Your gateway already validates the JWT and forwards the resolved user as an
`X-User-Id` header to downstream services (tweet-service, social-service, etc).
chat-service follows the exact same pattern for REST calls.

For the **WebSocket handshake**, the same thing happens: the gateway's existing
auth filter must run on the `/chat-service/ws/**` route too, so `X-User-Id` is
present on the HTTP request before it gets upgraded to a WebSocket. See
`ChatHandshakeInterceptor` - it reads that header once, at handshake time, and
the resulting userId becomes the STOMP session's `Principal` for its entire
lifetime (see `StompAuthChannelInterceptor`). Nothing after that re-reads
headers; the principal travels with the session.

**chat-service trusts `X-User-Id` completely and does not validate JWTs itself.**
This only works if chat-service is unreachable except through the gateway -
same assumption your other services already make.

## Project layout

```
chat-service/
├── pom.xml
├── Dockerfile
├── docker-compose-snippet.yml          # merge into your existing docker-compose.yml
├── gateway-config-reference/
│   └── gateway-routes-chat-service.yml # routes to add to your Gateway project
└── src/main/
    ├── resources/
    │   ├── application.yml
    │   └── db/migration/V1__init_chat_schema.sql
    └── java/com/twitterx/chatservice/
        ├── ChatServiceApplication.java
        ├── config/
        │   ├── SecurityConfig.java
        │   ├── WebSocketConfig.java              # STOMP endpoint + broker config
        │   ├── ChatHandshakeInterceptor.java      # reads X-User-Id at handshake
        │   └── StompAuthChannelInterceptor.java   # sets STOMP Principal on CONNECT
        ├── security/
        │   ├── UserPrincipal.java
        │   ├── GatewayUserHeaderFilter.java       # reads X-User-Id for REST calls
        │   └── CurrentUserProvider.java
        ├── controller/
        │   ├── ChatWebSocketController.java       # @MessageMapping handlers
        │   └── ConversationController.java        # REST: create/list conversations, history
        ├── service/
        │   ├── ConversationService.java
        │   └── MessageService.java
        ├── entity/        # Conversation, ConversationParticipant, Message
        ├── repository/
        ├── dto/
        ├── enums/
        └── exception/
```

## Setup

1. **Database**: create `chat_service_db` in Postgres (or use the docker-compose
   snippet). Flyway runs migrations automatically on startup - no manual DDL needed.

2. **Gateway routes**: add the two routes from
   `gateway-config-reference/gateway-routes-chat-service.yml` to your gateway's
   `application.yml`. The WS route uses `lb:ws://chat-service` (not `lb://`) -
   this matters, a plain `lb://` route won't upgrade the connection correctly.

3. **Eureka**: chat-service registers itself automatically via
   `spring-cloud-starter-netflix-eureka-client`, same as your other services -
   no extra config needed beyond what's in `application.yml`.

4. Run it:
   ```bash
   cd chat-service
   ./mvnw spring-boot:run
   ```

## REST API (via gateway: `/chat-service/api/v1/...`)

| Method | Path                                      | Purpose                          |
|--------|--------------------------------------------|-----------------------------------|
| POST   | `/conversations`                            | Create 1:1 or group conversation |
| GET    | `/conversations`                            | List my conversations            |
| GET    | `/conversations/{id}/messages?page=&size=`  | Paginated message history        |
| POST   | `/conversations/{id}/read?lastReadMessageId=` | Mark conversation read         |

**Create 1:1:**
```json
POST /conversations
{ "type": "ONE_TO_ONE", "participantIds": [42] }
```

**Create group:**
```json
POST /conversations
{ "type": "GROUP", "participantIds": [42, 43, 44], "groupName": "Project Falcon" }
```

## WebSocket / STOMP (via gateway: `ws://.../chat-service/ws`)

**Client → Server** (`/app/...`):

| Destination             | Payload                                                   |
|--------------------------|------------------------------------------------------------|
| `/app/chat.sendMessage`  | `{ conversationId, content, messageType }`                 |
| `/app/chat.typing`       | `{ conversationId }`                                        |
| `/app/chat.stopTyping`   | `{ conversationId }`                                        |
| `/app/chat.markRead`     | `{ conversationId, lastReadMessageId }`                      |

**Server → Client** (subscribe per conversation):
```
/topic/conversations.{conversationId}
```
You'll receive either a `ChatMessageResponse` (new message) or a `WsEvent`
(typing / stop-typing / read-receipt) on that same topic - check the shape or
add a `kind` discriminator client-side if you want to be strict about it.

### Sample browser client (for manual testing)

```html
<script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
<script>
  const socket = new SockJS('http://localhost:8080/chat-service/ws?token=' + JWT);
  const stompClient = Stomp.over(socket);

  stompClient.connect({}, () => {
    const conversationId = 5;

    stompClient.subscribe(`/topic/conversations.${conversationId}`, (frame) => {
      console.log('Received:', JSON.parse(frame.body));
    });

    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
      conversationId: conversationId,
      content: 'hey!',
      messageType: 'TEXT'
    }));
  });
</script>
```

## Scaling beyond one instance

The current setup uses Spring's **simple in-memory STOMP broker**
(`registry.enableSimpleBroker(...)` in `WebSocketConfig`). That means if you
run multiple chat-service instances behind the gateway, a message sent by a
user connected to instance A will NOT reach a user connected to instance B,
because the broker (and the list of subscriptions) lives in instance A's
memory only.

This is fine for development and a single instance. When you're ready to
scale chat-service horizontally, swap the simple broker for an external
STOMP-capable broker (RabbitMQ with the STOMP plugin is the most common
pairing with Spring): change `enableSimpleBroker` to
`enableStompBrokerRelay("/topic", "/queue")` and point it at RabbitMQ. No
changes needed anywhere else - controllers and DTOs stay the same.

## Design notes / things you may want to extend

- **Online/offline presence** wasn't in scope for this version (you picked
  1:1 + group only) - if you add it later, hook into
  `ApplicationListener<SessionConnectEvent>` / `SessionDisconnectEvent` and
  broadcast a `USER_JOINED`/`USER_LEFT` `WsEvent` - the enum value already
  exists for this.
- **Group membership changes** (add/remove member, leave group) aren't
  exposed yet - `ConversationParticipant.leftAt` and `isAdmin` are already
  modeled for this, just need the endpoints.
- **Validating that participantIds are real users** currently isn't done -
  `CreateConversationRequest` accepts any userId. Wire up a Feign call to
  auth-service (`spring-cloud-starter-openfeign` is already in `pom.xml`)
  if you want to reject conversations with nonexistent users.
- **Notifications**: when a message is sent to an offline user, you'll
  probably want chat-service to call notification-service (push notification)
  - that's a natural Feign client to add next.
