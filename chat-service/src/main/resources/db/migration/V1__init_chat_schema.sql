-- ============================================================
-- chat-service core schema
-- conversations can be ONE_TO_ONE or GROUP
-- a conversation has many participants (users, by id only - no FK,
-- since users live in auth-service)
-- ============================================================

CREATE TABLE conversations (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('ONE_TO_ONE', 'GROUP')),
    name            VARCHAR(150),                 -- only used for GROUP chats
    created_by      BIGINT       NOT NULL,         -- userId of creator
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),

    -- for ONE_TO_ONE conversations only: a deterministic key so we can
    -- enforce "only one conversation between user A and user B"
    direct_key      VARCHAR(64)  UNIQUE
);

CREATE TABLE conversation_participants (
    id                BIGSERIAL PRIMARY KEY,
    conversation_id   BIGINT      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id           BIGINT      NOT NULL,
    joined_at         TIMESTAMP   NOT NULL DEFAULT now(),
    last_read_message_id BIGINT,                  -- for unread-count / read-receipt tracking
    is_admin          BOOLEAN     NOT NULL DEFAULT FALSE,  -- relevant for GROUP chats
    left_at           TIMESTAMP,                  -- null while still a member

    CONSTRAINT uq_conversation_user UNIQUE (conversation_id, user_id)
);

CREATE INDEX idx_participants_user_id ON conversation_participants(user_id);
CREATE INDEX idx_participants_conversation_id ON conversation_participants(conversation_id);

CREATE TABLE messages (
    id                BIGSERIAL PRIMARY KEY,
    conversation_id   BIGINT      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id         BIGINT      NOT NULL,
    content           TEXT        NOT NULL,
    message_type      VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'SYSTEM')),
    status            VARCHAR(20) NOT NULL DEFAULT 'SENT' CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    created_at        TIMESTAMP   NOT NULL DEFAULT now(),
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_messages_conversation_id_created_at ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
